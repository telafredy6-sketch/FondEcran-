package com.fredy.fondecran

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
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

    // Palette de couleurs exacte correspondant au thème TypeScript
    private val bgColor = Color.parseColor("#E3F2FD")
    private val topBarColor = Color.parseColor("#BBDEFB")
    private val mainBlue = Color.parseColor("#1E88E5")
    private val darkBlue = Color.parseColor("#0D47A1")
    private val subtitleBlue = Color.parseColor("#1565C0")
    private val cardBorderColor = Color.parseColor("#DBEAFE")

    private enum class Page {
        HOME,
        PROJECT_EDIT,
        PINTEREST_FEED,
        SYSTEM_SOUNDS,
        SETTINGS
    }

    private var currentPage = Page.HOME
    private var activeProject: SlideshowProject? = null
    private var editingSlideForMusic: SlideItem? = null

    private lateinit var rootContainer: FrameLayout
    private lateinit var contentContainer: LinearLayout
    private lateinit var bottomNav: LinearLayout

    private var pinterestCategory = "all"
    private var systemSoundsCategory = "all"
    private var tempSystemSoundPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = topBarColor

        rootContainer = FrameLayout(this)
        setContentView(rootContainer)

        // Charger les projets persistants
        val projects = Slides.loadProjects(this)
        activeProject = Slides.getActiveProject(this)

        // Sécurité au démarrage si configurée
        val config = Slides.loadAppConfig(this)
        if (config.securityPinEnabled) {
            SecurityGateDialog(this, config.securityPin) {
                setupMainInterface()
            }.show()
        } else {
            setupMainInterface()
        }
    }

    private fun setupMainInterface() {
        rootContainer.removeAllViews()

        // 1. Fond d'écran personnalisé de l'application
        val bgView = View(this).apply {
            setBackgroundColor(bgColor)
            layoutParams = ViewGroup.LayoutParams(match, match)
        }
        Slides.applyAppWallpaper(bgView, this)
        rootContainer.addView(bgView)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(match, match)
        }

        // 2. En-tête de l'application (Header Bar)
        val appBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0BBDEFB"))
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(match, wrap)
        }

        // Bouton Retour si on n'est pas sur Accueil
        if (currentPage != Page.HOME) {
            val backBtn = Button(this).apply {
                text = "←"
                textSize = 18f
                setTextColor(darkBlue)
                setBackgroundColor(Color.TRANSPARENT)
                minWidth = 0; minimumWidth = 0; setPadding(0, 0, dp(8), 0)
                setOnClickListener {
                    currentPage = Page.HOME
                    setupBottomNav()
                    renderCurrentPage()
                }
            }
            appBar.addView(backBtn)
        }

        // Logo Icon avec dégradé bleu
        val logoIcon = TextView(this).apply {
            text = "FE"
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(0, 0, dp(10), 0) }
            background = GradientDrawable().apply {
                colors = intArrayOf(darkBlue, mainBlue)
                orientation = GradientDrawable.Orientation.TL_BR
                cornerRadius = dp(11).toFloat()
            }
        }
        appBar.addView(logoIcon)

        // Titre et badge Studio
        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        }
        titleBox.addView(TextView(this).apply {
            text = "Fond d'écran"
            textSize = 17f
            setTextColor(darkBlue)
        })
        titleBox.addView(TextView(this).apply {
            text = "Studio"
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(dp(6), 0, 0, 0) }
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(6).toFloat()
            }
        })
        appBar.addView(titleBox)

        // Bouton Aperçu Téléphone
        val btnLivePreview = Button(this).apply {
            text = "📱 Aperçu Téléphone"
            setAllCaps(false)
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                val proj = activeProject ?: return@setOnClickListener
                LiveWallpaperPreviewDialog(this@MainActivity, proj).show()
            }
        }
        appBar.addView(btnLivePreview)
        mainLayout.addView(appBar)

        // 3. Contenu Défilant
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(match, 0, 1f)
        }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(contentContainer)
        mainLayout.addView(scroll)

        // 4. Barre de Navigation Inférieure (Bottom Nav)
        bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
            }
            elevation = dp(8).toFloat()
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = LinearLayout.LayoutParams(match, wrap)
        }
        setupBottomNav()
        mainLayout.addView(bottomNav)

        rootContainer.addView(mainLayout)
        renderCurrentPage()
    }

    private fun setupBottomNav() {
        bottomNav.removeAllViews()
        val navItems = listOf(
            Triple(Page.HOME, "Accueil", "🏠"),
            Triple(Page.PROJECT_EDIT, "Édition", "🎛️"),
            Triple(Page.PINTEREST_FEED, "Pinterest", "✨"),
            Triple(Page.SYSTEM_SOUNDS, "Sons", "🔔"),
            Triple(Page.SETTINGS, "Réglages", "⚙️")
        )

        navItems.forEach { (page, label, icon) ->
            val isCurrent = currentPage == page
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener {
                    currentPage = page
                    setupMainInterface()
                }
            }

            item.addView(TextView(this).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
            })

            item.addView(TextView(this).apply {
                text = label
                textSize = 10f
                setTextColor(if (isCurrent) mainBlue else Color.GRAY)
                gravity = Gravity.CENTER
            })

            bottomNav.addView(item)
        }
    }

    private fun renderCurrentPage() {
        contentContainer.removeAllViews()
        when (currentPage) {
            Page.HOME -> renderHomePage()
            Page.PROJECT_EDIT -> renderProjectEditPage()
            Page.PINTEREST_FEED -> renderPinterestFeedPage()
            Page.SYSTEM_SOUNDS -> renderSystemSoundsPage()
            Page.SETTINGS -> renderSettingsPage()
        }
    }

    // ==========================================
    // 1. PAGE ACCUEIL (Identique HomePage.tsx)
    // ==========================================

    private fun renderHomePage() {
        // Hero Welcome Banner
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                colors = intArrayOf(mainBlue, darkBlue)
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = dp(24).toFloat()
            }
            setPadding(dp(22), dp(22), dp(22), dp(22))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(18)) }
        }

        // Pill
        val pill = TextView(this).apply {
            text = "✨ Créateur Pro de Fonds d'Écran & Diaporamas"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(0, 0, 0, dp(8)) }
        }
        hero.addView(pill)

        hero.addView(TextView(this).apply {
            text = "Fond d'écran Studio"
            textSize = 28f
            setTextColor(Color.WHITE)
        })

        hero.addView(TextView(this).apply {
            text = "Crée des diaporamas animés avec musiques découpées sur-mesure, durées par image personnalisées et prévisualisation smartphone en direct."
            textSize = 12f
            setTextColor(Color.parseColor("#E0F2FE"))
            setPadding(0, dp(4), 0, dp(14))
        })

        val btnNew = Button(this).apply {
            text = "+ Nouveau Projet"
            setAllCaps(false)
            textSize = 14f
            setTextColor(darkBlue)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
            }
            setOnClickListener { showNewProjectDialog() }
        }
        hero.addView(btnNew)
        contentContainer.addView(hero)

        // En-tête section Projets
        val projHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(10)) }
        }
        val projects = Slides.loadProjects(this)
        val projTitle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        }
        projTitle.addView(TextView(this).apply {
            text = "Mes Diaporamas & Fonds"
            textSize = 18f
            setTextColor(darkBlue)
        })
        projTitle.addView(TextView(this).apply {
            text = "${projects.size} projet(s) sauvegardé(s)"
            textSize = 12f
            setTextColor(mainBlue)
        })
        projHeader.addView(projTitle)

        val btnPinterestLink = Button(this).apply {
            text = "Explorer Pinterest →"
            setAllCaps(false)
            textSize = 12f
            setTextColor(mainBlue)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                currentPage = Page.PINTEREST_FEED
                setupMainInterface()
            }
        }
        projHeader.addView(btnPinterestLink)
        contentContainer.addView(projHeader)

        // Liste des cartes de projets
        projects.forEach { proj ->
            contentContainer.addView(buildProjectCard(proj))
        }
    }

    private fun buildProjectCard(proj: SlideshowProject): View {
        val isActive = proj.id == activeProject?.id
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(isActive.let { if (it) 2 else 1 }), if (isActive) mainBlue else cardBorderColor)
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(10)) }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Miniature
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(72))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = dp(14).toFloat()
            }
            if (proj.slides.isNotEmpty()) {
                val f = File(Slides.slidesDir(this@MainActivity), proj.slides[0].fileName)
                if (f.exists()) {
                    val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                    setImageBitmap(BitmapFactory.decodeFile(f.absolutePath, o))
                }
            }
        }
        topRow.addView(thumb)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(dp(14), 0, dp(8), 0) }
        }

        // Mode pill
        val pillRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pillRow.addView(TextView(this).apply {
            text = if (proj.mode == "slideshow") "DIAPORAMA" else "IMAGE UNIQUE"
            textSize = 9f
            setTextColor(if (proj.mode == "slideshow") darkBlue else Color.parseColor("#166534"))
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = GradientDrawable().apply {
                setColor(if (proj.mode == "slideshow") Color.parseColor("#DBEAFE") else Color.parseColor("#DCFCE7"))
                cornerRadius = dp(6).toFloat()
            }
        })
        if (isActive) {
            pillRow.addView(TextView(this).apply {
                text = "Actif"
                textSize = 9f
                setTextColor(Color.parseColor("#92400E"))
                setPadding(dp(6), dp(2), dp(6), dp(2))
                layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(dp(6), 0, 0, 0) }
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FEF3C7"))
                    cornerRadius = dp(6).toFloat()
                }
            })
        }
        info.addView(pillRow)

        info.addView(TextView(this).apply {
            text = proj.title
            textSize = 16f
            setTextColor(darkBlue)
            setPadding(0, dp(3), 0, dp(2))
        })

        info.addView(TextView(this).apply {
            text = "${proj.slides.size} images • ${proj.defaultDurationSec}s par défaut"
            textSize = 12f
            setTextColor(Color.GRAY)
        })
        topRow.addView(info)
        card.addView(topRow)

        // Boutons bas de carte
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(12), 0, 0) }
        }

        val editBtn = Button(this).apply {
            text = "Éditer le projet →"
            setAllCaps(false)
            textSize = 12f
            setTextColor(darkBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E3F2FD"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(0, 0, dp(6), 0) }
            setOnClickListener {
                activeProject = proj
                Slides.setActiveProject(this@MainActivity, proj)
                currentPage = Page.PROJECT_EDIT
                setupMainInterface()
            }
        }
        btnRow.addView(editBtn)

        val playBtn = Button(this).apply {
            text = "▶ Lancer"
            setAllCaps(false)
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(wrap, wrap)
            setOnClickListener {
                activeProject = proj
                Slides.setActiveProject(this@MainActivity, proj)
                LiveWallpaperPreviewDialog(this@MainActivity, proj).show()
            }
        }
        btnRow.addView(playBtn)
        card.addView(btnRow)

        return card
    }

    private fun showNewProjectDialog() {
        val options = arrayOf("1. Diaporama (Plusieurs images avec durée)", "2. Image Unique (Fond ultra haute qualité)")
        AlertDialog.Builder(this)
            .setTitle("Nouveau Projet")
            .setItems(options) { _, which ->
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
                currentPage = Page.PROJECT_EDIT
                setupMainInterface()
            }
            .show()
    }

    // ==========================================
    // 2. PAGE ÉDITION PROJET (Identique ProjectEditPage.tsx)
    // ==========================================

    private fun renderProjectEditPage() {
        val proj = activeProject ?: return renderHomePage()

        // Carte En-tête Projet
        val topCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), cardBorderColor)
            }
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(14)) }
        }

        val modeLabel = TextView(this).apply {
            text = if (proj.mode == "slideshow") "MODE DIAPORAMA MULTI-IMAGES" else "MODE IMAGE UNIQUE"
            textSize = 10f
            setTextColor(if (proj.mode == "slideshow") darkBlue else Color.parseColor("#166534"))
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = GradientDrawable().apply {
                setColor(if (proj.mode == "slideshow") Color.parseColor("#DBEAFE") else Color.parseColor("#DCFCE7"))
                cornerRadius = dp(8).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(0, 0, 0, dp(8)) }
        }
        topCard.addView(modeLabel)

        val titleInput = EditText(this).apply {
            setText(proj.title)
            textSize = 20f
            setTextColor(darkBlue)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(0, dp(2), 0, dp(6))
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    proj.title = text.toString()
                    saveCurrentProject()
                }
            }
        }
        topCard.addView(titleInput)

        val btnPreviewTop = Button(this).apply {
            text = if (proj.mode == "slideshow") "▶ Lancer Diaporama" else "📱 Prévisualiser"
            setAllCaps(false)
            textSize = 13f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                LiveWallpaperPreviewDialog(this@MainActivity, proj).show()
            }
        }
        topCard.addView(btnPreviewTop)
        contentContainer.addView(topCard)

        // Réglages Cible (Accueil, Verrouillage, Les deux)
        val targetCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), cardBorderColor)
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(14)) }
        }
        targetCard.addView(TextView(this).apply {
            text = "Appliquer sur :"
            textSize = 13f
            setTextColor(darkBlue)
            setPadding(0, 0, 0, dp(6))
        })
        val targetRadio = radios(arrayOf("Écran d'accueil", "Écran de verrouillage", "Les deux"), proj.globalTarget)
        targetRadio.setOnCheckedChangeListener { _, _ ->
            proj.globalTarget = selected(targetRadio)
            saveCurrentProject()
        }
        targetCard.addView(targetRadio)
        contentContainer.addView(targetCard)

        // Liste des Diapositives
        val slidesHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(10)) }
        }
        slidesHeader.addView(TextView(this).apply {
            text = "Images & Réglages Individuels (${proj.slides.size})"
            textSize = 16f
            setTextColor(darkBlue)
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        })
        val addBtn = Button(this).apply {
            text = "+ Ajouter"
            setAllCaps(false)
            textSize = 12f
            setTextColor(mainBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E3F2FD"))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener { pick("image/*", true, reqImages) }
        }
        slidesHeader.addView(addBtn)
        contentContainer.addView(slidesHeader)

        proj.slides.forEachIndexed { i, slide ->
            contentContainer.addView(buildSlideRow(i, slide))
        }

        if (proj.slides.isEmpty()) {
            val emptyBox = TextView(this).apply {
                text = "Aucune image dans ce projet.\nTouche « + Ajouter » pour importer des images."
                textSize = 13f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(30), dp(20), dp(30))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(16).toFloat()
                }
            }
            contentContainer.addView(emptyBox)
        }

        // Actions globales en bas
        val bottomActions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(16), 0, 0) }
        }

        bottomActions.addView(Button(this).apply {
            text = "Appliquer maintenant sur le téléphone"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(14).toFloat()
            }
            setOnClickListener {
                thread {
                    val ok = Slides.applyNext(applicationContext)
                    runOnUiThread { toast(if (ok) "Fond d'écran changé ✔" else "Ajoute d'abord des images") }
                }
            }
        })

        if (proj.mode == "slideshow") {
            bottomActions.addView(Button(this).apply {
                text = "Démarrer le diaporama automatique"
                setAllCaps(false)
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(mainBlue)
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(8), 0, 0) }
                setOnClickListener {
                    startSlideWork(proj.defaultDurationSec)
                    toast("Diaporama automatique démarré ✔")
                }
            })
        }
        contentContainer.addView(bottomActions)
    }

    private fun buildSlideRow(i: Int, slide: SlideItem): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), cardBorderColor)
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(8)) }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Thumbnail
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(70))
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(12).toFloat()
            }
            val f = File(Slides.slidesDir(this@MainActivity), slide.fileName)
            if (f.exists()) {
                val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                setImageBitmap(BitmapFactory.decodeFile(f.absolutePath, o))
            }
        }
        top.addView(thumb)

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(dp(12), 0, dp(8), 0) }
        }
        info.addView(TextView(this).apply {
            text = "Image ${i + 1} • ${slide.name}"
            textSize = 14f
            setTextColor(darkBlue)
        })

        // Music badge
        info.addView(TextView(this).apply {
            text = if (slide.audio != null) "♪ ${slide.audio?.name} (${slide.audio?.let { it.endTimeSec - it.startTimeSec }}s)" else "Aucun son associé"
            textSize = 11f
            setTextColor(if (slide.audio != null) Color.parseColor("#15803D") else Color.GRAY)
            setPadding(0, dp(3), 0, 0)
        })
        top.addView(info)
        row.addView(top)

        // Ligne de boutons individuels
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(10), 0, 0) }
        }

        // Bouton Durée Individuelle
        val durBtn = Button(this).apply {
            text = "⏱ ${slide.durationSec}s"
            setAllCaps(false)
            textSize = 11f
            setTextColor(darkBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { showDurationPicker(slide) }
        }
        actions.addView(durBtn)

        // Bouton Son
        val musicBtn = Button(this).apply {
            text = "♪ Son"
            setAllCaps(false)
            textSize = 11f
            setTextColor(if (slide.audio != null) Color.parseColor("#15803D") else mainBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(dp(4), 0, dp(4), 0) }
            setOnClickListener {
                editingSlideForMusic = slide
                AudioEditorDialog(this@MainActivity, slide, { pick("audio/*", false, reqMusic) }) { clip ->
                    slide.audio = clip
                    saveCurrentProject()
                    renderCurrentPage()
                    toast("Son associé ✔")
                }.show()
            }
        }
        actions.addView(musicBtn)

        // Bouton Retouches
        val editBtn = Button(this).apply {
            text = "✎ Modifier"
            setAllCaps(false)
            textSize = 11f
            setTextColor(darkBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = dp(10).toFloat()
            }
            setOnClickListener {
                ImageEditorDialog(this@MainActivity, slide, { adj ->
                    slide.adjustments = adj
                    saveCurrentProject()
                    renderCurrentPage()
                    toast("Retouches enregistrées ✔")
                }) { bgFileName ->
                    Slides.setAppWallpaper(this@MainActivity, bgFileName)
                    setupMainInterface()
                }.show()
            }
        }
        actions.addView(editBtn)

        // Spacer
        actions.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })

        // Bouton Supprimer
        val delBtn = Button(this).apply {
            text = "🗑"
            textSize = 14f
            setTextColor(Color.parseColor("#DC2626"))
            setBackgroundColor(Color.TRANSPARENT)
            minWidth = 0; minimumWidth = 0; setPadding(dp(6), 0, dp(6), 0)
            setOnClickListener {
                activeProject?.slides?.removeAt(i)
                saveCurrentProject()
                renderCurrentPage()
            }
        }
        actions.addView(delBtn)
        row.addView(actions)

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
                renderCurrentPage()
            }
            .show()
    }

    // ==========================================
    // 3. PAGE DÉCOUVERTE PINTEREST
    // ==========================================

    private fun renderPinterestFeedPage() {
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                colors = intArrayOf(Color.parseColor("#E11D48"), Color.parseColor("#BE123C"))
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                cornerRadius = dp(24).toFloat()
            }
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(14)) }
        }

        banner.addView(TextView(this).apply {
            text = "Découverte Pinterest"
            textSize = 24f
            setTextColor(Color.WHITE)
        })

        banner.addView(TextView(this).apply {
            text = "Explore et importe des fonds d'écran tendances ou applique-les directement dans l'application."
            textSize = 12f
            setTextColor(Color.parseColor("#FFE4E6"))
            setPadding(0, dp(4), 0, dp(12))
        })

        val searchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val searchInput = EditText(this).apply {
            hint = "Chercher nature, océan, anime..."
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(0, 0, dp(8), 0) }
        }
        searchRow.addView(searchInput)

        searchRow.addView(Button(this).apply {
            text = "Rechercher"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener { openPinterest(searchInput.text.toString()) }
        })
        banner.addView(searchRow)
        contentContainer.addView(banner)

        // Catégories chips
        val chipRow = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(12)) }
        }
        val chipContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cats = listOf(
            Pair("all", "Toutes les tendances"),
            Pair("nature", "🌿 Nature"),
            Pair("ocean", "🌊 Océan"),
            Pair("anime", "✨ Anime"),
            Pair("cyberpunk", "⚡ Cyberpunk"),
            Pair("minimal", "🎨 Minimaliste"),
            Pair("space", "🌌 Espace")
        )
        cats.forEach { (catId, label) ->
            val isSel = pinterestCategory == catId
            val b = Button(this).apply {
                text = label
                setAllCaps(false)
                textSize = 11f
                setTextColor(if (isSel) Color.WHITE else darkBlue)
                background = GradientDrawable().apply {
                    setColor(if (isSel) mainBlue else Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                    if (!isSel) setStroke(dp(1), cardBorderColor)
                }
                layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(0, 0, dp(6), 0) }
                setOnClickListener {
                    pinterestCategory = catId
                    renderCurrentPage()
                }
            }
            chipContainer.addView(b)
        }
        chipRow.addView(chipContainer)
        contentContainer.addView(chipRow)

        // Liste des cartes
        val filtered = Presets.PINTEREST_EXPLORE_FEED.filter {
            pinterestCategory == "all" || it.category == pinterestCategory
        }

        filtered.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(18).toFloat()
                    setStroke(dp(1), cardBorderColor)
                }
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(10)) }
            }

            val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            titleRow.addView(TextView(this).apply {
                text = item.title
                textSize = 15f
                setTextColor(darkBlue)
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
            })
            titleRow.addView(TextView(this).apply {
                text = "📌 Pinterest"
                textSize = 10f
                setTextColor(Color.parseColor("#E11D48"))
            })
            card.addView(titleRow)

            card.addView(TextView(this).apply {
                text = item.tags.joinToString(" • ") { "#$it" }
                textSize = 11f
                setTextColor(Color.GRAY)
                setPadding(0, dp(2), 0, dp(10))
            })

            val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actionRow.addView(Button(this).apply {
                text = "+ Ajouter au projet"
                setAllCaps(false)
                textSize = 12f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(mainBlue)
                    cornerRadius = dp(10).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(0, 0, dp(6), 0) }
                setOnClickListener {
                    val proj = activeProject
                    if (proj != null) {
                        proj.slides.add(SlideItem(name = item.title, fileName = "starter_1.jpg"))
                        saveCurrentProject()
                        toast("« ${item.title} » ajouté à ${proj.title} ✔")
                    }
                }
            })

            actionRow.addView(Button(this).apply {
                text = "Fond d'App"
                setAllCaps(false)
                textSize = 12f
                setTextColor(darkBlue)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E3F2FD"))
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener {
                    Slides.setAppWallpaper(this@MainActivity, "starter_1.jpg")
                    setupMainInterface()
                    toast("Fond d'application défini ✔")
                }
            })
            card.addView(actionRow)
            contentContainer.addView(card)
        }
    }

    // ==========================================
    // 4. PAGE SONS SYSTÈME
    // ==========================================

    private fun renderSystemSoundsPage() {
        contentContainer.addView(TextView(this).apply {
            text = "Sons Système & Sonneries"
            textSize = 24f
            setTextColor(darkBlue)
        })

        contentContainer.addView(TextView(this).apply {
            text = "Personnalise les alertes sonores, sonneries et alarmes de ton téléphone."
            textSize = 13f
            setTextColor(subtitleBlue)
            setPadding(0, dp(2), 0, dp(14))
        })

        Presets.SYSTEM_SOUNDS_CATALOG.forEach { s ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), cardBorderColor)
                }
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(8)) }
            }

            val icon = TextView(this).apply {
                text = when (s.category) {
                    "notification" -> "🔔"
                    "ringtone" -> "📞"
                    else -> "⏰"
                }
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E3F2FD"))
                    cornerRadius = dp(12).toFloat()
                }
            }
            card.addView(icon)

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(dp(12), 0, dp(8), 0) }
            }
            info.addView(TextView(this).apply {
                text = s.title
                textSize = 15f
                setTextColor(darkBlue)
            })
            info.addView(TextView(this).apply {
                text = "${s.category.uppercase()} • ${s.durationLabel}"
                textSize = 11f
                setTextColor(Color.GRAY)
            })
            card.addView(info)

            val playBtn = Button(this).apply {
                text = "▶ Écouter"
                setAllCaps(false)
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(mainBlue)
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener {
                    tempSystemSoundPlayer?.release()
                    try {
                        val uri = when (s.category) {
                            "notification" -> android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                            "ringtone" -> android.provider.Settings.System.DEFAULT_RINGTONE_URI
                            else -> android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                        }
                        tempSystemSoundPlayer = MediaPlayer.create(this@MainActivity, uri)?.apply {
                            start()
                        }
                    } catch (e: Exception) {
                        toast("Lecture du son...")
                    }
                }
            }
            card.addView(playBtn)
            contentContainer.addView(card)
        }
    }

    // ==========================================
    // 5. PAGE PARAMÈTRES
    // ==========================================

    private fun renderSettingsPage() {
        contentContainer.addView(TextView(this).apply {
            text = "Paramètres de l'Application"
            textSize = 24f
            setTextColor(darkBlue)
            setPadding(0, 0, 0, dp(14))
        })

        val config = Slides.loadAppConfig(this)

        // 1. Fond d'écran interne de l'application
        val cardWp = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), cardBorderColor)
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(12)) }
        }
        cardWp.addView(TextView(this).apply {
            text = "Fond d'écran interne de l'application"
            textSize = 16f
            setTextColor(darkBlue)
        })

        cardWp.addView(TextView(this).apply {
            text = if (config.appWallpaperFileName != null) "Fond personnalisé actif" else "Fond par défaut (bleu doux)"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(4), 0, dp(8))
        })

        if (config.appWallpaperFileName != null) {
            cardWp.addView(Button(this).apply {
                text = "Retirer le fond d'écran d'application"
                setAllCaps(false)
                setTextColor(Color.parseColor("#DC2626"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FEE2E2"))
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener {
                    Slides.setAppWallpaper(this@MainActivity, null)
                    toast("Fond d'écran par défaut rétabli")
                    setupMainInterface()
                }
            })
        }
        contentContainer.addView(cardWp)

        // 2. Sécurité & Authentification
        val cardSec = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), cardBorderColor)
            }
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(12)) }
        }
        cardSec.addView(TextView(this).apply {
            text = "Sécurité & Authentification"
            textSize = 16f
            setTextColor(darkBlue)
        })

        val pinCheck = CheckBox(this).apply {
            text = "Protéger l'ouverture par code PIN / empreinte"
            setTextColor(darkBlue)
            isChecked = config.securityPinEnabled
            setOnCheckedChangeListener { _, checked ->
                config.securityPinEnabled = checked
                Slides.saveAppConfig(this@MainActivity, config)
                toast(if (checked) "Verrouillage activé (Code : ${config.securityPin})" else "Verrouillage désactivé")
            }
        }
        cardSec.addView(pinCheck)

        cardSec.addView(Button(this).apply {
            text = "Modifier le code PIN (${config.securityPin})"
            setAllCaps(false)
            setTextColor(mainBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E3F2FD"))
                cornerRadius = dp(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(wrap, wrap).apply { setMargins(0, dp(8), 0, 0) }
            setOnClickListener { showChangePinDialog(config) }
        })
        contentContainer.addView(cardSec)
    }

    private fun showChangePinDialog(config: AppConfig) {
        val input = EditText(this).apply {
            hint = "4 chiffres (ex: 1234)"
            setSingleLine(true)
            textSize = 18f
            gravity = Gravity.CENTER
        }
        AlertDialog.Builder(this)
            .setTitle("Nouveau code PIN")
            .setView(input)
            .setPositiveButton("Enregistrer") { _, _ ->
                val p = input.text.toString().trim()
                if (p.length == 4) {
                    config.securityPin = p
                    Slides.saveAppConfig(this, config)
                    renderCurrentPage()
                    toast("Code PIN mis à jour : $p")
                } else {
                    toast("Le code PIN doit comporter exactement 4 chiffres")
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    // ==========================================
    // Utilitaires de données & actions
    // ==========================================

    private fun saveCurrentProject() {
        val proj = activeProject ?: return
        val projects = Slides.loadProjects(this)
        val idx = projects.indexOfFirst { it.id == proj.id }
        if (idx >= 0) projects[idx] = proj else projects.add(proj)
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
        val url = if (q.isBlank()) "https://www.pinterest.com/"
        else "https://www.pinterest.com/search/pins/?q=" + Uri.encode(q.trim() + " wallpaper")
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
            if (resultCode == RESULT_OK) setupMainInterface() else finish()
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
                    renderCurrentPage()
                    toast("Images ajoutées avec succès ✔")
                }
            }
        } else if (requestCode == reqMusic) {
            val uri = data.data ?: return
            val slide = editingSlideForMusic ?: return
            thread {
                val res = Slides.importAudio(applicationContext, uri)
                if (res != null) {
                    slide.audio = AudioClip(
                        name = res.second,
                        fileName = res.first,
                        isSynth = false
                    )
                    saveCurrentProject()
                }
                runOnUiThread {
                    renderCurrentPage()
                    toast("Fichier audio importé ✔")
                }
            }
        }
    }

    override fun onBackPressed() {
        if (currentPage != Page.HOME) {
            currentPage = Page.HOME
            setupMainInterface()
        } else {
            super.onBackPressed()
        }
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
        tempSystemSoundPlayer?.release()
        super.onDestroy()
    }
}
