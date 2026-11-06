package com.fredy.fondecran

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val reqImages = 1
    private val reqMusic = 2
    private val intervals = intArrayOf(15, 30, 60, 360)

    private lateinit var info: TextView
    private lateinit var target: RadioGroup
    private lateinit var every: RadioGroup
    private lateinit var musicOn: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        col.addView(label("Fond d'écran", 24f))
        info = label("", 14f)
        col.addView(info)

        col.addView(button("1. Choisir des images") { pick("image/*", true, reqImages) })
        col.addView(button("2. Choisir une musique (facultatif)") { pick("audio/*", false, reqMusic) })

        col.addView(label("Appliquer sur :", 16f))
        target = radios(arrayOf("Écran d'accueil", "Écran de verrouillage", "Les deux"), 2)
        col.addView(target)

        col.addView(label("Changer toutes les :", 16f))
        every = radios(arrayOf("15 min", "30 min", "1 h", "6 h"), 0)
        col.addView(every)

        musicOn = CheckBox(this).apply {
            text = "Musique activée (fond animé)"
            isChecked = Slides.prefs(this@MainActivity).getBoolean("music_on", true)
        }
        col.addView(musicOn)

        col.addView(label("A. Diaporama (change le vrai fond d'écran)", 16f))
        col.addView(button("Appliquer maintenant") { applyNow() })
        col.addView(button("Démarrer le diaporama") { startSlideshow() })
        col.addView(button("Arrêter le diaporama") {
            WorkManager.getInstance(this).cancelUniqueWork("slides")
            toast("Diaporama arrêté")
        })

        col.addView(label("B. Fond d'écran animé avec musique", 16f))
        col.addView(button("Activer le fond animé") { openLive() })

        setContentView(ScrollView(this).apply { addView(col) })
        refresh()
    }

    // ---------- actions ----------

    private fun pick(mime: String, multiple: Boolean, code: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mime
            addCategory(Intent.CATEGORY_OPENABLE)
            if (multiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Choisir"), code)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        if (requestCode == reqImages) {
            val uris = ArrayList<Uri>()
            val clip = data.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
            } else {
                data.data?.let { uris.add(it) }
            }
            toast("Import en cours…")
            thread {
                val n = Slides.importImages(applicationContext, uris)
                runOnUiThread {
                    refresh()
                    toast("$n image(s) importée(s)")
                }
            }
        } else if (requestCode == reqMusic) {
            val uri = data.data ?: return
            thread {
                try {
                    Slides.importMusic(applicationContext, uri)
                } catch (e: Exception) {
                }
                runOnUiThread {
                    refresh()
                    toast("Musique importée")
                }
            }
        }
    }

    private fun saveSettings() {
        Slides.prefs(this).edit()
            .putInt("target", selected(target))
            .putInt("interval_min", intervals[selected(every)])
            .putBoolean("music_on", musicOn.isChecked)
            .apply()
    }

    private fun applyNow() {
        saveSettings()
        thread {
            val ok = try {
                Slides.applyNext(applicationContext)
            } catch (e: Exception) {
                false
            }
            runOnUiThread { toast(if (ok) "Fond d'écran changé ✔" else "Choisis d'abord des images") }
        }
    }

    private fun startSlideshow() {
        saveSettings()
        if (Slides.files(this).isEmpty()) {
            toast("Choisis d'abord des images")
            return
        }
        val req = PeriodicWorkRequest.Builder(
            SlideWorker::class.java, intervals[selected(every)].toLong(), TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("slides", ExistingPeriodicWorkPolicy.UPDATE, req)
        toast("Diaporama démarré")
        thread {
            try {
                Slides.applyNext(applicationContext)
            } catch (e: Exception) {
            }
        }
    }

    private fun openLive() {
        saveSettings()
        if (Slides.files(this).isEmpty()) {
            toast("Choisis d'abord des images")
            return
        }
        try {
            val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, LiveWallpaperService::class.java)
            )
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }

    private fun refresh() {
        val music = if (Slides.musicFile(this).exists())
            Slides.prefs(this).getString("music_name", "musique") else "aucune"
        info.text = "Images : ${Slides.files(this).size}\nMusique : $music"
    }

    // ---------- petits helpers d'interface ----------

    private fun label(t: String, size: Float) = TextView(this).apply {
        text = t
        textSize = size
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setOnClickListener { onClick() }
    }

    private fun radios(labels: Array<String>, checked: Int): RadioGroup {
        val g = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        for (l in labels) {
            g.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = l
            })
        }
        (g.getChildAt(checked) as RadioButton).isChecked = true
        return g
    }

    private fun selected(g: RadioGroup): Int =
        g.indexOfChild(g.findViewById<View>(g.checkedRadioButtonId))

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
