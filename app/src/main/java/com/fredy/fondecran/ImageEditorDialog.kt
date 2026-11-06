package com.fredy.fondecran

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import java.io.File

class ImageEditorDialog(
    context: Context,
    private val slide: SlideItem,
    private val onSave: (ImageAdjustments) -> Unit,
    private val onSetAsAppBg: (String) -> Unit
) : Dialog(context) {

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private val mainBlue = Color.parseColor("#1E88E5")
    private val darkBlue = Color.parseColor("#0D47A1")

    private val adj = slide.adjustments.copy()
    private var baseBitmap: Bitmap? = null
    private lateinit var previewImage: ImageView
    private lateinit var tabContentContainer: LinearLayout

    private var activeTab = "crop" // crop, adjust, filters, bg

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val f = File(Slides.slidesDir(context), slide.fileName)
        if (f.exists()) {
            baseBitmap = BitmapFactory.decodeFile(f.absolutePath)
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(24).toFloat()
            }
            layoutParams = ViewGroup.LayoutParams(match, wrap)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Header
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = "Éditeur d'Image : ${slide.name}"
            textSize = 18f
            setTextColor(darkBlue)
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        })
        val closeBtn = Button(context).apply {
            text = "✕"
            setTextColor(Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dismiss() }
        }
        header.addView(closeBtn)
        root.addView(header)

        // Preview Viewport with frame
        val previewFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(match, dp(260)).apply {
                setMargins(0, dp(10), 0, dp(10))
            }
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(16).toFloat()
            }
        }

        previewImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(match, match)
        }
        previewFrame.addView(previewImage)
        root.addView(previewFrame)
        refreshPreview()

        // Tab Navigation Bar (Format, Ajuster, Filtres, Fond)
        val tabRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(4), 0, dp(8)) }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F1F5F9"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val tabs = listOf(Pair("crop", "Format"), Pair("adjust", "Ajuster"), Pair("filters", "Filtres"), Pair("bg", "Fond"))
        tabs.forEach { (tabId, label) ->
            val b = Button(context).apply {
                text = label
                setAllCaps(false)
                textSize = 12f
                setTextColor(if (activeTab == tabId) mainBlue else Color.DKGRAY)
                background = GradientDrawable().apply {
                    setColor(if (activeTab == tabId) Color.WHITE else Color.TRANSPARENT)
                    cornerRadius = dp(10).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
                setOnClickListener {
                    activeTab = tabId
                    updateTabContent()
                    // Re-render tab buttons
                    for (i in 0 until tabRow.childCount) {
                        val btn = tabRow.getChildAt(i) as Button
                        val sel = tabs[i].first == activeTab
                        btn.setTextColor(if (sel) mainBlue else Color.DKGRAY)
                        btn.background = GradientDrawable().apply {
                            setColor(if (sel) Color.WHITE else Color.TRANSPARENT)
                            cornerRadius = dp(10).toFloat()
                        }
                    }
                }
            }
            tabRow.addView(b)
        }
        root.addView(tabRow)

        // Dynamic Tab Content Container
        tabContentContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(match, wrap)
        }
        root.addView(tabContentContainer)
        updateTabContent()

        // Bottom Actions
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(12), 0, 0) }
        }

        val appBgBtn = Button(context).apply {
            text = "Fond d'App"
            setAllCaps(false)
            setTextColor(mainBlue)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E3F2FD"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(0, 0, dp(6), 0) }
            setOnClickListener {
                onSetAsAppBg(slide.fileName)
                Toast.makeText(context, "Défini comme fond d'écran de l'application !", Toast.LENGTH_SHORT).show()
            }
        }
        actionRow.addView(appBgBtn)

        val saveBtn = Button(context).apply {
            text = "Enregistrer"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1.2f)
            setOnClickListener {
                onSave(adj)
                dismiss()
            }
        }
        actionRow.addView(saveBtn)
        root.addView(actionRow)

        setContentView(root)
    }

    private fun refreshPreview() {
        val bmp = baseBitmap ?: return
        val filtered = Slides.processFilteredBitmap(bmp, adj)
        previewImage.setImageBitmap(filtered)
    }

    private fun updateTabContent() {
        tabContentContainer.removeAllViews()

        when (activeTab) {
            "crop" -> {
                // Zoom
                tabContentContainer.addView(label("Zoom : ${String.format("%.2f", adj.zoom)}x"))
                tabContentContainer.addView(SeekBar(context).apply {
                    max = 40
                    progress = ((adj.zoom - 1.0f) * 20).toInt()
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                            adj.zoom = 1.0f + (p / 20f)
                            refreshPreview()
                        }
                        override fun onStartTrackingTouch(b: SeekBar?) {}
                        override fun onStopTrackingTouch(b: SeekBar?) {}
                    })
                })

                // Pan X
                tabContentContainer.addView(label("Déplacement Horizontal (X)"))
                tabContentContainer.addView(SeekBar(context).apply {
                    max = 200
                    progress = (adj.panX + 100).toInt()
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                            adj.panX = (p - 100).toFloat()
                            refreshPreview()
                        }
                        override fun onStartTrackingTouch(b: SeekBar?) {}
                        override fun onStopTrackingTouch(b: SeekBar?) {}
                    })
                })

                // Pan Y
                tabContentContainer.addView(label("Déplacement Vertical (Y)"))
                tabContentContainer.addView(SeekBar(context).apply {
                    max = 200
                    progress = (adj.panY + 100).toInt()
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                            adj.panY = (p - 100).toFloat()
                            refreshPreview()
                        }
                        override fun onStartTrackingTouch(b: SeekBar?) {}
                        override fun onStopTrackingTouch(b: SeekBar?) {}
                    })
                })
            }
            "adjust" -> {
                // Luminosité
                tabContentContainer.addView(label("Luminosité : ${adj.brightness}%"))
                tabContentContainer.addView(SeekBar(context).apply {
                    max = 100
                    progress = adj.brightness - 50
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                            adj.brightness = p + 50
                            refreshPreview()
                        }
                        override fun onStartTrackingTouch(b: SeekBar?) {}
                        override fun onStopTrackingTouch(b: SeekBar?) {}
                    })
                })

                // Contraste
                tabContentContainer.addView(label("Contraste : ${adj.contrast}%"))
                tabContentContainer.addView(SeekBar(context).apply {
                    max = 100
                    progress = adj.contrast - 50
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                            adj.contrast = p + 50
                            refreshPreview()
                        }
                        override fun onStartTrackingTouch(b: SeekBar?) {}
                        override fun onStopTrackingTouch(b: SeekBar?) {}
                    })
                })

                // Saturation
                tabContentContainer.addView(label("Saturation : ${adj.saturation}%"))
                tabContentContainer.addView(SeekBar(context).apply {
                    max = 200
                    progress = adj.saturation
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                            adj.saturation = p
                            refreshPreview()
                        }
                        override fun onStartTrackingTouch(b: SeekBar?) {}
                        override fun onStopTrackingTouch(b: SeekBar?) {}
                    })
                })
            }
            "filters" -> {
                val grid = GridLayout(context).apply {
                    columnCount = 3
                    layoutParams = LinearLayout.LayoutParams(match, wrap)
                }
                val filterList = listOf(
                    Triple("normal", "Normal", "✨"),
                    Triple("vivid", "Vif", "🌈"),
                    Triple("bw", "N&B", "🌑"),
                    Triple("vintage", "Rétro", "🎞️"),
                    Triple("warm", "Chaud", "🌅"),
                    Triple("cyberpunk", "Néon", "⚡")
                )
                filterList.forEach { (fid, name, emoji) ->
                    val b = Button(context).apply {
                        text = "$emoji $name"
                        setAllCaps(false)
                        textSize = 12f
                        setTextColor(if (adj.filterPreset == fid) Color.WHITE else darkBlue)
                        background = GradientDrawable().apply {
                            setColor(if (adj.filterPreset == fid) mainBlue else Color.parseColor("#E3F2FD"))
                            cornerRadius = dp(10).toFloat()
                        }
                        setOnClickListener {
                            applyFilterPreset(fid)
                            updateTabContent()
                        }
                    }
                    grid.addView(b)
                }
                tabContentContainer.addView(grid)
            }
            "bg" -> {
                tabContentContainer.addView(label("Couleur d'arrière-plan du cadrage"))
                val colors = listOf("#000000", "#111827", "#0D47A1", "#1E88E5", "#4C1D95", "#831843", "#FFFFFF")
                val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                colors.forEach { hex ->
                    val v = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor(hex))
                            cornerRadius = dp(8).toFloat()
                            if (adj.bgColor == hex) setStroke(dp(3), mainBlue)
                        }
                        setOnClickListener {
                            adj.bgColor = hex
                            refreshPreview()
                            updateTabContent()
                        }
                    }
                    row.addView(v)
                }
                tabContentContainer.addView(row)
            }
        }
    }

    private fun applyFilterPreset(preset: String) {
        adj.filterPreset = preset
        when (preset) {
            "vivid" -> { adj.contrast = 125; adj.saturation = 145 }
            "bw" -> { adj.saturation = 0; adj.contrast = 115 }
            "vintage" -> { adj.brightness = 95; adj.contrast = 110; adj.saturation = 75 }
            "warm" -> { adj.brightness = 105; adj.saturation = 120 }
            "cyberpunk" -> { adj.contrast = 135; adj.saturation = 150 }
            else -> { adj.brightness = 100; adj.contrast = 100; adj.saturation = 100 }
        }
        refreshPreview()
    }

    private fun label(t: String) = TextView(context).apply {
        text = t
        textSize = 13f
        setTextColor(darkBlue)
        setPadding(0, dp(4), 0, dp(2))
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
}
