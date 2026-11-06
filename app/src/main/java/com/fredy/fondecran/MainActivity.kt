package com.fredy.fondecran

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val reqImages = 1
    private val reqAuth = 2
    private val reqMusic = 3

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private val bgColor = Color.parseColor("#E3F2FD")
    private val mainBlue = Color.parseColor("#1E88E5")
    private val darkBlue = Color.parseColor("#0D47A1")

    // Navigation Pages
    private enum class Page {
        HOME,
        PROJECT_EDIT,
        IMAGE_EDITOR,
        AUDIO_EDITOR,
        SYSTEM_SOUNDS,
        SETTINGS
    }

    private var currentPage = Page.HOME
    private var activeProject: SlideshowProject? = null
    private var editingSlide: SlideItem? = null
    private var tempMediaPlayer: MediaPlayer? = null

    private lateinit var rootContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#BBDEFB")

        rootContainer = FrameLayout(this)
        setContentView(rootContainer)

        // Authentification par empreinte ou code si configuré
        val pinEnabled = Slides.prefs(this).getBoolean("security_pin_enabled", false)
        if (pinEnabled) {
            val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val intent = if (km.isDeviceSecure) {
                km.createConfirmDeviceCredentialIntent(
                    "Fond d'écran", "Confirme ton identité pour ouvrir l'app"
                )
            } else null

            if (intent != null) {
                startActivityForResult(intent, reqAuth)
                return
            }
        }

        navigateTo(Page.HOME)
    }

    private fun navigateTo(page: Page) {
        currentPage = page
        rootContainer.removeAllViews()

        // Appliquer le fond personnalisé si défini par l'utilisateur
        val bgView = View(this).apply {
            setBackgroundColor(bgColor)
            layoutParams = ViewGroup.LayoutParams(match, match)
        }
        Slides.applyAppWallpaper(bgView, this)
        rootContainer.addView(bgView)

        val pageView = when (page) {
            Page.HOME -> buildHomePage()
            Page.PROJECT_EDIT -> buildProjectEditPage()
            Page.IMAGE_EDITOR -> buildImageEditorPage()
            Page.AUDIO_EDITOR -> buildAudioEditorPage()
            Page.SYSTEM_SOUNDS -> buildSystemSoundsPage()
            Page.SETTINGS -> buildSettingsPage()
        }

        val scroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(match, match)
            addView(pageView)
        }
        rootContainer.addView(scroll)
    }

    override fun onBackPressed() {
        if (currentPage != Page.HOME) {
            navigateTo(Page.HOME)
        } else {
            super.onBackPressed()
        }
    }

    // ==========================================
    // 1. PAGE ACCUEIL / LISTE DES DIAPORAMAS
    // ==========================================

    private fun buildHomePage(): View {
        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        // Header
        col.addView(label("Fond d'écran Studio", 26f, darkBlue))
        col.addView(label("Gère tes diaporamas et fonds d'écran avec musique", 13f, Color.parseColor("#1565C0")))

        // Bouton Nouveau Projet
        val btnNew = button("+ Nouveau Diaporama / Fond") {
            showNewProjectDialog()
        }
        col.addView(btnNew)

        // Accès rapides
        val rowShortcuts = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(12)) }
        }
        rowShortcuts.addView(smallTextButton("🎵 Sons Système") { navigateTo(Page.SYSTEM_SOUNDS) })
        rowShortcuts.addView(smallTextButton("⚙️ Paramètres") { navigateTo(Page.SETTINGS) })
        col.addView(rowShortcuts)

        // Liste des projets
        val projects = Slides.loadProjects(this)
        col.addView(label("Mes Projets (${projects.size})", 18f, darkBlue))

        for (proj in projects) {
            col.addView(buildProjectCard(proj))
        }

        // Section Pinterest
        col.addView(label("Découverte Pinterest", 18f, darkBlue))
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
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(6), 0, dp(6)) }
        }
        col.addView(search)
        col.addView(button("Chercher sur Pinterest") { openPinterest(search.text.toString()) })

        return col
    }

    private fun buildProjectCard(proj: SlideshowProject): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(6), 0, dp(6)) }
        }

        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(72))
            if (proj.slides.isNotEmpty()) {
                val f = File(Slides.slidesDir(this@MainActivity), proj.slides[0].fileName)
                if (f.exists()) {
                    val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                    setImageBitmap(BitmapFactory.decodeFile(f.absolutePath, o))
                }
            } else {
                setBackgroundColor(Color.LTGRAY)
            }
        }
        card.addView(thumb)

        val infoBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(dp(12), 0, dp(12), 0) }
        }
        infoBox.addView(TextView(this).apply {
            text = proj.title
            textSize = 16f
            setTextColor(darkBlue)
        })
        infoBox.addView(TextView(this).apply {
            text = "${proj.slides.size} image(s) • ${if (proj.mode == "slideshow") "Diaporama" else "Image unique"}"
            textSize = 12f
            setTextColor(Color.GRAY)
        })
        card.addView(infoBox)

        // Bouton Éditer
        val editBtn = Button(this).apply {
            text = "Éditer"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener {
                activeProject = proj
                Slides.setActiveProject(this@MainActivity, proj)
                navigateTo(Page.PROJECT_EDIT)
            }
        }
        card.addView(editBtn)

        return card
    }

    private fun showNewProjectDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nouveau Projet")
        builder.setItems(arrayOf("1. Diaporama (Plusieurs images)", "2. Image Unique")) { _, which ->
            val mode = if (which == 0) "slideshow" else "single"
            val newProj = SlideshowProject(
                id = "proj_" + System.currentTimeMillis(),
                title = if (mode == "slideshow") "Nouveau Diaporama" else "Nouveau Fond Unique",
                mode = mode
            )
            val projects = Slides.loadProjects(this)
            projects.add(0, newProj)
            Slides.saveProjects(this, projects)
            activeProject = newProj
            Slides.setActiveProject(this, newProj)
            navigateTo(Page.PROJECT_EDIT)
        }
        builder.show()
    }

    // ==========================================
    // 2. PAGE ÉDITION DU DIAPORAMA / FOND
    // ==========================================

    private fun buildProjectEditPage(): View {
        val proj = activeProject ?: return buildHomePage()
        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        col.addView(smallTextButton("← Retour à l'accueil") { navigateTo(Page.HOME) })

        col.addView(label(proj.title, 24f, darkBlue))
        col.addView(label("Mode : ${if (proj.mode == "slideshow") "Diaporama" else "Image Unique"}", 13f, Color.parseColor("#1565C0")))

        // Bouton Ajouter des images
        col.addView(button("1. Choisir / Ajouter des images") {
            pick("image/*", true, reqImages)
        })

        col.addView(label("Images du projet (${proj.slides.size})", 18f, darkBlue))

        val slidesBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        proj.slides.forEachIndexed { index, slide ->
            slidesBox.addView(buildSlideRow(index, slide))
        }
        if (proj.slides.isEmpty()) {
            slidesBox.addView(label("Aucune image. Touche « 1. Choisir / Ajouter des images ».", 14f, darkBlue))
        }
        col.addView(slidesBox)

        // Paramètres de diaporama
        col.addView(label("Appliquer sur :", 16f, darkBlue))
        val targetRadio = radios(arrayOf("Écran d'accueil", "Écran de verrouillage", "Les deux"), proj.target)
        col.addView(targetRadio)

        // Actions
        col.addView(button("Appliquer maintenant") {
            proj.target = selected(targetRadio)
            saveCurrentProject()
            thread {
                val ok = Slides.applyNext(applicationContext)
                runOnUiThread { toast(if (ok) "Fond d'écran changé ✔" else "Ajoute d'abord des images") }
            }
        })

        if (proj.mode == "slideshow") {
            col.addView(button("Démarrer le diaporama automatique") {
                proj.target = selected(targetRadio)
                saveCurrentProject()
                startSlideWork(proj.defaultDurationSec)
                toast("Diaporama démarré")
            })

            col.addView(button("Arrêter le diaporama") {
                WorkManager.getInstance(this).cancelUniqueWork("slides")
                toast("Diaporama arrêté")
            })
        }

        col.addView(button("Activer le fond animé en direct") {
            proj.target = selected(targetRadio)
            saveCurrentProject()
            openLiveWallpaper()
        })

        return col
    }

    private fun buildSlideRow(i: Int, slide: SlideItem): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(4)) }
        }

        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(64))
            val f = File(Slides.slidesDir(this@MainActivity), slide.fileName)
            if (f.exists()) {
                val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                setImageBitmap(BitmapFactory.decodeFile(f.absolutePath, o))
            }
        }
        row.addView(thumb)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(dp(8), 0, dp(8), 0) }
        }
        info.addView(TextView(this).apply {
            text = "Image ${i + 1} (${slide.durationSec}s)"
            textSize = 14f
            setTextColor(darkBlue)
        })
        info.addView(TextView(this).apply {
            text = if (slide.audioName != null) "♪ ${slide.audioName}" else "Aucun son"
            textSize = 12f
            setTextColor(if (slide.audioName != null) Color.parseColor("#2E7D32") else Color.GRAY)
        })
        row.addView(info)

        // Bouton Durée Individuelle
        val durBtn = smallButton("${slide.durationSec}s") {
            showDurationPicker(slide)
        }
        row.addView(durBtn)

        // Bouton Son / Trimmer
        val musicBtn = smallButton("♪") {
            editingSlide = slide
            navigateTo(Page.AUDIO_EDITOR)
        }
        row.addView(musicBtn)

        // Bouton Retouches / Image Editor
        val editBtn = smallButton("✎") {
            editingSlide = slide
            navigateTo(Page.IMAGE_EDITOR)
        }
        row.addView(editBtn)

        // Bouton Supprimer
        val delBtn = smallButton("✕") {
            activeProject?.slides?.removeAt(i)
            saveCurrentProject()
            navigateTo(Page.PROJECT_EDIT)
        }
        row.addView(delBtn)

        return row
    }

    private fun showDurationPicker(slide: SlideItem) {
        val options = arrayOf("5 secondes", "10 secondes", "15 secondes", "30 secondes", "1 minute", "5 minutes", "15 minutes", "1 heure")
        val seconds = intArrayOf(5, 10, 15, 30, 60, 300, 900, 3600)
        AlertDialog.Builder(this)
            .setTitle("Durée individuelle de cette image")
            .setItems(options) { _, which ->
                slide.durationSec = seconds[which]
                saveCurrentProject()
                navigateTo(Page.PROJECT_EDIT)
                toast("Durée réglée à ${options[which]}")
            }
            .show()
    }

    // ==========================================
    // 3. PAGE ÉDITEUR D'IMAGE (RECADRAGE & FILTRES)
    // ==========================================

    private fun buildImageEditorPage(): View {
        val slide = editingSlide ?: return buildHomePage()
        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        col.addView(smallTextButton("← Retour au projet") { navigateTo(Page.PROJECT_EDIT) })
        col.addView(label("Éditeur d'Image : ${slide.name}", 22f, darkBlue))

        val imgFile = File(Slides.slidesDir(this), slide.fileName)
        var currentBitmap = if (imgFile.exists()) BitmapFactory.decodeFile(imgFile.absolutePath) else null

        val preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(match, dp(260))
            if (currentBitmap != null) {
                setImageBitmap(Slides.processFilteredBitmap(currentBitmap!!, slide))
            }
        }
        col.addView(preview)

        val refreshPreview = {
            if (currentBitmap != null) {
                preview.setImageBitmap(Slides.processFilteredBitmap(currentBitmap!!, slide))
            }
        }

        // Sliders d'ajustements
        col.addView(label("Luminosité : ${slide.brightness}%", 14f, darkBlue))
        val seekBri = SeekBar(this).apply {
            max = 100
            progress = slide.brightness - 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, fromUser: Boolean) {
                    slide.brightness = p + 50
                    refreshPreview()
                }
                override fun onStartTrackingTouch(b: SeekBar?) {}
                override fun onStopTrackingTouch(b: SeekBar?) {}
            })
        }
        col.addView(seekBri)

        col.addView(label("Contraste : ${slide.contrast}%", 14f, darkBlue))
        val seekCon = SeekBar(this).apply {
            max = 100
            progress = slide.contrast - 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, fromUser: Boolean) {
                    slide.contrast = p + 50
                    refreshPreview()
                }
                override fun onStartTrackingTouch(b: SeekBar?) {}
                override fun onStopTrackingTouch(b: SeekBar?) {}
            })
        }
        col.addView(seekCon)

        col.addView(label("Saturation : ${slide.saturation}%", 14f, darkBlue))
        val seekSat = SeekBar(this).apply {
            max = 200
            progress = slide.saturation
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, fromUser: Boolean) {
                    slide.saturation = p
                    refreshPreview()
                }
                override fun onStartTrackingTouch(b: SeekBar?) {}
                override fun onStopTrackingTouch(b: SeekBar?) {}
            })
        }
        col.addView(seekSat)

        // Filtres 1-clic
        val rowFilters = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(match, wrap)
        }
        rowFilters.addView(smallTextButton("Normal") {
            slide.brightness = 100; slide.contrast = 100; slide.saturation = 100
            seekBri.progress = 50; seekCon.progress = 50; seekSat.progress = 100
            refreshPreview()
        })
        rowFilters.addView(smallTextButton("Vif") {
            slide.contrast = 125; slide.saturation = 150
            seekCon.progress = 75; seekSat.progress = 150
            refreshPreview()
        })
        rowFilters.addView(smallTextButton("N&B") {
            slide.saturation = 0; seekSat.progress = 0
            refreshPreview()
        })
        rowFilters.addView(smallTextButton("Rétro") {
            slide.brightness = 95; slide.contrast = 110; slide.saturation = 80
            refreshPreview()
        })
        col.addView(rowFilters)

        // Bouton Fond de l'application
        col.addView(button("Mettre en fond de cette application") {
            Slides.setAppWallpaperPath(this, imgFile.absolutePath)
            toast("Fond d'écran appliqué dans l'app !")
        })

        // Bouton Sauvegarder
        col.addView(button("Sauvegarder les retouches") {
            saveCurrentProject()
            navigateTo(Page.PROJECT_EDIT)
            toast("Retouches enregistrées ✔")
        })

        return col
    }

    // ==========================================
    // 4. PAGE ÉDITEUR DE SON (DÉCOUPE & VOLUME)
    // ==========================================

    private fun buildAudioEditorPage(): View {
        val slide = editingSlide ?: return buildHomePage()
        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        col.addView(smallTextButton("← Retour au projet") {
            stopPreviewAudio()
            navigateTo(Page.PROJECT_EDIT)
        })

        col.addView(label("Éditeur de Son", 22f, darkBlue))
        col.addView(label("Son actuel : ${slide.audioName ?: "Aucun"}", 14f, Color.parseColor("#1565C0")))

        col.addView(button("Choisir un fichier audio") {
            pick("audio/*", false, reqMusic)
        })

        // Contrôles de découpe (Start & End)
        col.addView(label("Début de l'extrait : ${slide.audioStartMs / 1000}s", 14f, darkBlue))
        val seekStart = SeekBar(this).apply {
            max = 60
            progress = slide.audioStartMs / 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    slide.audioStartMs = p * 1000
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        col.addView(seekStart)

        col.addView(label("Volume : ${(slide.audioVolume * 100).toInt()}%", 14f, darkBlue))
        val seekVol = SeekBar(this).apply {
            max = 100
            progress = (slide.audioVolume * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    slide.audioVolume = p / 100f
                    tempMediaPlayer?.setVolume(slide.audioVolume, slide.audioVolume)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        col.addView(seekVol)

        // Pré-écoute
        col.addView(button("▶ Écouter l'extrait") {
            val audioName = slide.audioFileName ?: return@button
            val f = File(Slides.audioDir(this), audioName)
            if (!f.exists()) return@button
            stopPreviewAudio()
            try {
                tempMediaPlayer = MediaPlayer().apply {
                    setDataSource(f.absolutePath)
                    setVolume(slide.audioVolume, slide.audioVolume)
                    isLooping = true
                    prepare()
                    seekTo(slide.audioStartMs)
                    start()
                }
            } catch (e: Exception) {
            }
        })

        col.addView(button("⏹ Arrêter l'écoute") {
            stopPreviewAudio()
        })

        col.addView(button("Sauvegarder et Associer") {
            stopPreviewAudio()
            saveCurrentProject()
            navigateTo(Page.PROJECT_EDIT)
            toast("Son associé avec succès ✔")
        })

        return col
    }

    private fun stopPreviewAudio() {
        tempMediaPlayer?.stop()
        tempMediaPlayer?.release()
        tempMediaPlayer = null
    }

    // ==========================================
    // 5. PAGE SONS SYSTÈME (SONNERIES & NOTIFS)
    // ==========================================

    private fun buildSystemSoundsPage(): View {
        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        col.addView(smallTextButton("← Retour à l'accueil") { navigateTo(Page.HOME) })
        col.addView(label("Sons Système & Sonneries", 22f, darkBlue))
        col.addView(label("Personnalise les alertes sonores de ton téléphone", 13f, Color.parseColor("#1565C0")))

        val sounds = arrayOf(
            Triple("Goutte d’eau pure", "Notification", android.provider.Settings.System.DEFAULT_NOTIFICATION_URI),
            Triple("Carillon doux", "Sonnerie", android.provider.Settings.System.DEFAULT_RINGTONE_URI),
            Triple("Réveil Matin", "Alarme", android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
        )

        for (s in sounds) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(6), 0, dp(6)) }
            }

            val t = TextView(this).apply {
                text = "${s.first}\n(${s.second})"
                textSize = 14f
                setTextColor(darkBlue)
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
            }
            row.addView(t)

            val testBtn = smallButton("▶") {
                try {
                    val p = MediaPlayer.create(this@MainActivity, s.third)
                    p?.start()
                } catch (e: Exception) {
                    toast("Lecture du son système")
                }
            }
            row.addView(testBtn)

            col.addView(row)
        }

        return col
    }

    // ==========================================
    // 6. PAGE PARAMÈTRES DE L'APPLICATION
    // ==========================================

    private fun buildSettingsPage(): View {
        val pad = dp(16)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        col.addView(smallTextButton("← Retour à l'accueil") { navigateTo(Page.HOME) })
        col.addView(label("Paramètres", 22f, darkBlue))

        // Fond d'écran interne de l'application
        col.addView(label("Fond d'écran interne de l'application", 16f, darkBlue))
        val curAppWp = Slides.getAppWallpaperPath(this)
        col.addView(TextView(this).apply {
            text = if (curAppWp != null) "Un fond personnalisé est actif" else "Fond par défaut (bleu doux)"
            setTextColor(Color.GRAY)
        })

        if (curAppWp != null) {
            col.addView(button("Réinitialiser le fond de l'application") {
                Slides.setAppWallpaperPath(this, null)
                toast("Fond par défaut rétabli")
                navigateTo(Page.SETTINGS)
            })
        }

        // Sécurité PIN
        col.addView(label("Sécurité et Verrouillage", 16f, darkBlue))
        val pinBox = CheckBox(this).apply {
            text = "Protéger l'ouverture par empreinte / code"
            setTextColor(darkBlue)
            isChecked = Slides.prefs(this@MainActivity).getBoolean("security_pin_enabled", false)
            setOnCheckedChangeListener { _, checked ->
                Slides.prefs(this@MainActivity).edit().putBoolean("security_pin_enabled", checked).apply()
                toast(if (checked) "Verrouillage activé" else "Verrouillage désactivé")
            }
        }
        col.addView(pinBox)

        return col
    }

    // ==========================================
    // Actions & Helpers
    // ==========================================

    private fun saveCurrentProject() {
        val proj = activeProject ?: return
        val projects = Slides.loadProjects(this)
        val idx = projects.indexOfFirst { it.id == proj.id }
        if (idx >= 0) {
            projects[idx] = proj
        } else {
            projects.add(proj)
        }
        Slides.saveProjects(this, projects)
    }

    private fun startSlideWork(intervalSec: Int) {
        val intervalMin = maxOf(15, intervalSec / 60)
        val req = PeriodicWorkRequest.Builder(
            SlideWorker::class.java, intervalMin.toLong(), TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("slides", ExistingPeriodicWorkPolicy.UPDATE, req)
    }

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
            if (resultCode == RESULT_OK) navigateTo(Page.HOME) else finish()
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
            toast("Importation de ${uris.size} image(s)…")
            thread {
                val proj = activeProject
                if (proj != null) {
                    for (u in uris) {
                        val slide = Slides.importImage(applicationContext, u)
                        if (slide != null) {
                            slide.durationSec = proj.defaultDurationSec
                            proj.slides.add(slide)
                        }
                    }
                    saveCurrentProject()
                }
                runOnUiThread {
                    navigateTo(Page.PROJECT_EDIT)
                    toast("Images ajoutées avec succès ✔")
                }
            }
        } else if (requestCode == reqMusic) {
            val uri = data.data ?: return
            val slide = editingSlide ?: return
            thread {
                val res = Slides.importAudio(applicationContext, uri)
                if (res != null) {
                    slide.audioFileName = res.first
                    slide.audioName = res.second
                    saveCurrentProject()
                }
                runOnUiThread {
                    navigateTo(Page.AUDIO_EDITOR)
                    toast("Musique importée ✔")
                }
            }
        }
    }

    private fun openLiveWallpaper() {
        val proj = activeProject
        if (proj == null || proj.slides.isEmpty()) {
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

    // ---------- UI Helper Functions ----------

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
        layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(6), 0, dp(6)) }
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
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { setMargins(dp(2), 0, dp(2), 0) }
        setOnClickListener { onClick() }
    }

    private fun smallTextButton(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setAllCaps(false)
        setTextColor(mainBlue)
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(10).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(0, 0, dp(8), 0) }
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

    override fun onDestroy() {
        stopPreviewAudio()
        super.onDestroy()
    }
}
