package com.fredy.fondecran

import android.app.Activity
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val reqImages = 1
    private val reqAuth = 2
    private val reqMusicBase = 100
    private val intervals = intArrayOf(15, 30, 60, 360)

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private val bgColor = Color.parseColor("#E3F2FD")
    private val mainBlue = Color.parseColor("#1E88E5")
    private val darkBlue = Color.parseColor("#0D47A1")

    private lateinit var info: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var target: RadioGroup
    private lateinit var every: RadioGroup
    private lateinit var musicOn: CheckBox
    private var built = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#BBDEFB")
        setContentView(View(this).apply { setBackgroundColor(bgColor) })

        // Authentification : empreinte ou code du téléphone
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val secure = km.isDeviceSecure
        val intent: Intent? = if (secure)
            km.createConfirmDeviceCredentialIntent(
                "Fond d'écran", "Confirme ton identité pour ouvrir l'app"
            ) else null

        if (intent == null) {
            buildUi()
            if (!secure) toast("Astuce : active un verrouillage d'écran pour protéger l'app")
        } else {
            startActivityForResult(intent, reqAuth)
        }
    }

    private fun buildUi() {
        if (built) return
        built = true

        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        col.addView(label("Fond d'écran", 26f, darkBlue))
        info = label("", 14f, darkBlue)
        col.addView(info)

        // --- Recherche d'images sur Pinterest ---
        col.addView(label("Trouver des images", 18f, darkBlue))
        val search = EditText(this).apply {
            hint = "Ex : nature, océan, anime"
            setSingleLine(true)
            setTextColor(darkBlue)
            setHintTextColor(Color.parseColor("#7A9CC6"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply {
                setMargins(0, dp(6), 0, dp(6))
            }
        }
        col.addView(search)
        col.addView(button("Chercher sur Pinterest") { openPinterest(search.text.toString()) })
        col.addView(
            label(
                "Dans Pinterest : ouvre une image, touche ⋯ puis « Télécharger l'image ». " +
                    "Reviens ici et touche « 1. Choisir des images ».",
                13f, darkBlue
            )
        )

        col.addView(button("1. Choisir des images") { pick("image/*", true, reqImages) })

        col.addView(label("2. Une musique par image", 18f, darkBlue))
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(listBox)

        col.addView(label("Appliquer sur :", 16f, darkBlue))
        target = radios(arrayOf("Écran d'accueil", "Écran de verrouillage", "Les deux"), 2)
        col.addView(target)

        col.addView(label("Changer toutes les :", 16f, darkBlue))
        every = radios(arrayOf("15 min", "30 min", "1 h", "6 h"), 0)
        col.addView(every)

        musicOn = CheckBox(this).apply {
            text = "Musique activée (fond animé)"
            setTextColor(darkBlue)
            isChecked = Slides.prefs(this@MainActivity).getBoolean("music_on", true)
        }
        col.addView(musicOn)

        col.addView(label("A. Diaporama (change le vrai fond d'écran)", 16f, darkBlue))
        col.addView(button("Appliquer maintenant") { applyNow() })
        col.addView(button("Démarrer le diaporama") { startSlideshow() })
        col.addView(button("Arrêter le diaporama") {
            WorkManager.getInstance(this).cancelUniqueWork("slides")
            toast("Diaporama arrêté")
        })

        col.addView(label("B. Fond d'écran animé avec musique", 16f, darkBlue))
        col.addView(button("Activer le fond animé") { openLive() })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(bgColor)
            addView(col)
        })
        refresh()
    }

    // ---------- actions ----------

    private fun openPinterest(q: String) {
        val url = if (q.isBlank()) {
            "https://www.pinterest.com/"
        } else {
            "https://www.pinterest.com/search/pins/?q=" + Uri.encode(q.trim() + " wallpaper")
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("Aucun navigateur trouvé")
        }
    }

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

        if (requestCode == reqAuth) {
            if (resultCode == RESULT_OK) buildUi() else finish()
            return
        }
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
        } else if (requestCode >= reqMusicBase) {
            val uri = data.data ?: return
            val list = Slides.files(this)
            val i = requestCode - reqMusicBase
            if (i !in list.indices) return
            val img = list[i]
            thread {
                try {
                    Slides.importMusic(applicationContext, img, uri)
                } catch (e: Exception) {
                }
                runOnUiThread {
                    refresh()
                    toast("Musique ajoutée à l'image ${i + 1}")
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

    // ---------- liste des images avec leur musique ----------

    private fun refresh() {
        val list = Slides.files(this)
        info.text = "Images : ${list.size}"
        listBox.removeAllViews()
        if (list.isEmpty()) {
            listBox.addView(label("Aucune image. Touche « 1. Choisir des images ».", 14f, darkBlue))
            return
        }
        list.forEachIndexed { i, f -> listBox.addView(row(i, f)) }
    }

    private fun row(i: Int, f: File): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply {
                setMargins(0, dp(6), 0, dp(6))
            }
        }

        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64))
            val o = BitmapFactory.Options().apply { inSampleSize = 8 }
            setImageBitmap(BitmapFactory.decodeFile(f.absolutePath, o))
        }

        val music = Slides.musicName(this, f)
        val txt = TextView(this).apply {
            text = "Image ${i + 1}\n" + (if (music != null) "♪ $music" else "Aucune musique")
            setTextColor(darkBlue)
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply {
                setMargins(dp(10), 0, dp(10), 0)
            }
        }

        val pickBtn = smallButton("♪") { pick("audio/*", false, reqMusicBase + i) }
        val delBtn = smallButton("✕") {
            Slides.removeMusic(this, f)
            refresh()
        }

        row.addView(thumb)
        row.addView(txt)
        row.addView(pickBtn)
        row.addView(delBtn)
        return row
    }

    // ---------- petits helpers d'interface ----------

    private fun label(t: String, size: Float, color: Int) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        setPadding(0, dp(8), 0, dp(4))
    }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setAllCaps(false)
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(mainBlue)
            cornerRadius = dp(12).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(match, wrap).apply {
            setMargins(0, dp(6), 0, dp(6))
        }
        setOnClickListener { onClick() }
    }

    private fun smallButton(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setAllCaps(false)
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply {
            setColor(mainBlue)
            cornerRadius = dp(10).toFloat()
        }
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
            setMargins(dp(3), 0, dp(3), 0)
        }
        setOnClickListener { onClick() }
    }

    private fun radios(labels: Array<String>, checked: Int): RadioGroup {
        val g = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        for (l in labels) {
            g.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = l
                setTextColor(darkBlue)
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
