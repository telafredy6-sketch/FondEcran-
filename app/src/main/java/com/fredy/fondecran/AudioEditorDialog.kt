package com.fredy.fondecran

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import java.io.File

class AudioEditorDialog(
    context: Context,
    private val slide: SlideItem,
    private val onPickFile: () -> Unit,
    private val onSave: (AudioClip) -> Unit
) : Dialog(context) {

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private val mainBlue = Color.parseColor("#1E88E5")
    private val darkBlue = Color.parseColor("#0D47A1")

    private val clip = slide.audio?.copy() ?: AudioClip(
        name = "Ambiance Lofi",
        isSynth = true,
        synthType = "lofi"
    )

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

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
            text = "Éditeur de Son : ${slide.name}"
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

        // Type Switch : Ambiance vs Fichier
        val rowType = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(8), 0, dp(8)) }
        }

        val btnSynth = Button(context).apply {
            text = "✨ Ambiance Relaxante"
            setAllCaps(false)
            setTextColor(if (clip.isSynth) Color.WHITE else darkBlue)
            background = GradientDrawable().apply {
                setColor(if (clip.isSynth) mainBlue else Color.parseColor("#E3F2FD"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(0, 0, dp(4), 0) }
            setOnClickListener {
                clip.isSynth = true
                dismiss()
                showSynthPicker()
            }
        }
        rowType.addView(btnSynth)

        val btnFile = Button(context).apply {
            text = "📁 Fichier Personnel"
            setAllCaps(false)
            setTextColor(if (!clip.isSynth) Color.WHITE else darkBlue)
            background = GradientDrawable().apply {
                setColor(if (!clip.isSynth) mainBlue else Color.parseColor("#E3F2FD"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f).apply { setMargins(dp(4), 0, 0, 0) }
            setOnClickListener {
                clip.isSynth = false
                dismiss()
                onPickFile()
            }
        }
        rowType.addView(btnFile)
        root.addView(rowType)

        // Current track name
        root.addView(TextView(context).apply {
            text = "Titre actuel : ${clip.name}"
            textSize = 14f
            setTextColor(Color.parseColor("#1565C0"))
            setPadding(0, dp(4), 0, dp(4))
        })

        // Waveform Visual Simulation Container
        val waveContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(match, dp(60)).apply { setMargins(0, dp(8), 0, dp(12)) }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        // 24 Barres d'ondes simulant la timeline
        for (i in 0 until 24) {
            val hRatio = 0.3f + (Math.sin(i * 0.7).toFloat() * 0.4f) + 0.3f
            val bar = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(6), (dp(40) * hRatio).toInt()).apply {
                    setMargins(dp(2), 0, dp(2), 0)
                }
                background = GradientDrawable().apply {
                    setColor(mainBlue)
                    cornerRadius = dp(3).toFloat()
                }
            }
            waveContainer.addView(bar)
        }
        root.addView(waveContainer)

        // Start Time Trimmer Slider
        val startTimeLabel = TextView(context).apply {
            text = "Point de départ : ${clip.startTimeSec}s"
            textSize = 13f
            setTextColor(darkBlue)
        }
        root.addView(startTimeLabel)

        val seekStart = SeekBar(context).apply {
            max = 60
            progress = clip.startTimeSec
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                    clip.startTimeSec = p
                    startTimeLabel.text = "Point de départ : ${p}s"
                }
                override fun onStartTrackingTouch(b: SeekBar?) {}
                override fun onStopTrackingTouch(b: SeekBar?) {}
            })
        }
        root.addView(seekStart)

        // Volume Slider
        val volumeLabel = TextView(context).apply {
            text = "Volume sonore : ${(clip.volume * 100).toInt()}%"
            textSize = 13f
            setTextColor(darkBlue)
        }
        root.addView(volumeLabel)

        val seekVol = SeekBar(context).apply {
            max = 100
            progress = (clip.volume * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, p: Int, f: Boolean) {
                    clip.volume = p / 100f
                    volumeLabel.text = "Volume sonore : ${p}%"
                    mediaPlayer?.setVolume(clip.volume, clip.volume)
                }
                override fun onStartTrackingTouch(b: SeekBar?) {}
                override fun onStopTrackingTouch(b: SeekBar?) {}
            })
        }
        root.addView(seekVol)

        // Playback Preview Button
        val playBtn = Button(context).apply {
            text = "▶ Écouter l'extrait découpé"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#27272A"))
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, dp(8), 0, dp(8)) }
            setOnClickListener {
                togglePlayback(this)
            }
        }
        root.addView(playBtn)

        // Save Button
        val saveBtn = Button(context).apply {
            text = "Associer ce son à l'image"
            setAllCaps(false)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(mainBlue)
                cornerRadius = dp(12).toFloat()
            }
            setOnClickListener {
                stopPlayback()
                onSave(clip)
                dismiss()
            }
        }
        root.addView(saveBtn)

        setContentView(root)
    }

    private fun showSynthPicker() {
        val options = Presets.AUDIO_AMBIENT_PRESETS.map { it.first }.toTypedArray()
        android.app.AlertDialog.Builder(context)
            .setTitle("Choisir une ambiance sonore")
            .setItems(options) { _, idx ->
                clip.name = options[idx]
                clip.synthType = Presets.AUDIO_AMBIENT_PRESETS[idx].second
                clip.isSynth = true
                onSave(clip)
            }
            .show()
    }

    private fun togglePlayback(btn: Button) {
        if (isPlaying) {
            stopPlayback()
            btn.text = "▶ Écouter l'extrait découpé"
        } else {
            val fileName = clip.fileName
            if (fileName != null) {
                val f = File(Slides.audioDir(context), fileName)
                if (f.exists()) {
                    try {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(f.absolutePath)
                            setVolume(clip.volume, clip.volume)
                            isLooping = true
                            prepare()
                            seekTo(clip.startTimeSec * 1000)
                            start()
                        }
                        isPlaying = true
                        btn.text = "⏹ Arrêter l'écoute"
                    } catch (e: Exception) {}
                }
            } else {
                Toast.makeText(context, "Ambiance sonore : ${clip.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
    }

    override fun onStop() {
        stopPlayback()
        super.onStop()
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
}
