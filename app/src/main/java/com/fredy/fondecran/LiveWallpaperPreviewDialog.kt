package com.fredy.fondecran

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveWallpaperPreviewDialog(
    context: Context,
    private val project: SlideshowProject,
    private val onSetAppBg: ((String) -> Unit)? = null
) : Dialog(context) {

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private val mainBlue = Color.parseColor("#1E88E5")

    private var currentIndex = 0
    private var isPlaying = true
    private var isMuted = false
    private var countdownSec = 15

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    private lateinit var wallpaperImage: ImageView
    private lateinit var clockTime: TextView
    private lateinit var clockDate: TextView
    private lateinit var countdownBadge: TextView
    private lateinit var musicPill: LinearLayout
    private lateinit var musicPillText: TextView

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (project.slides.isEmpty()) return

            val currentSlide = project.slides[currentIndex]
            countdownSec--
            countdownBadge.text = "${countdownSec}s"

            if (countdownSec <= 0) {
                currentIndex = (currentIndex + 1) % project.slides.size
                updateSlideView()
            }

            // Update clock
            val now = Date()
            clockTime.text = timeFormat.format(now)
            clockDate.text = dateFormat.format(now).replaceFirstChar { it.uppercase() }

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#F0000000")))

        val root = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(match, match)
        }

        // Top Floating Bar
        val topBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC18181B"))
                cornerRadius = dp(24).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(match, wrap).apply {
                gravity = Gravity.TOP
                setMargins(dp(16), dp(24), dp(16), 0)
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }

        val titleView = TextView(context).apply {
            text = project.title
            textSize = 14f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
        }
        topBar.addView(titleView)

        countdownBadge = TextView(context).apply {
            text = "15s"
            textSize = 12f
            setTextColor(Color.parseColor("#FDE047"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = dp(12).toFloat()
            }
        }
        topBar.addView(countdownBadge)

        val muteBtn = Button(context).apply {
            text = "🔊"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                isMuted = !isMuted
                text = if (isMuted) "🔇" else "🔊"
                if (isMuted) player?.pause() else player?.start()
            }
        }
        topBar.addView(muteBtn)

        val closeBtn = Button(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dismiss() }
        }
        topBar.addView(closeBtn)
        root.addView(topBar)

        // Smartphone Frame Center
        val phoneFrame = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(290), dp(530)).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#18181B"))
                cornerRadius = dp(36).toFloat()
                setStroke(dp(3), Color.parseColor("#3F3F46"))
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val screenArea = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(match, match)
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(28).toFloat()
            }
        }

        wallpaperImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(match, match)
        }
        screenArea.addView(wallpaperImage)

        // Lockscreen Overlay (Clock, Date, Music Pill, Swipe Bar)
        val lockOverlay = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(match, match)
            setPadding(dp(16), dp(48), dp(16), dp(24))
        }

        clockTime = TextView(context).apply {
            text = "12:00"
            textSize = 48f
            setTextColor(Color.WHITE)
            setShadowLayer(dp(4).toFloat(), 0f, 0f, Color.BLACK)
        }
        lockOverlay.addView(clockTime)

        clockDate = TextView(context).apply {
            text = "Vendredi 6 Novembre"
            textSize = 13f
            setTextColor(Color.parseColor("#E4E4E7"))
            setShadowLayer(dp(2).toFloat(), 0f, 0f, Color.BLACK)
        }
        lockOverlay.addView(clockDate)

        // Spacer
        lockOverlay.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(match, 0, 1f)
        })

        // Music Pill
        musicPill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#80000000"))
                cornerRadius = dp(16).toFloat()
            }
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        musicPill.addView(TextView(context).apply {
            text = "♪ "
            textSize = 14f
            setTextColor(mainBlue)
        })
        musicPillText = TextView(context).apply {
            text = "Musique"
            textSize = 12f
            setTextColor(Color.WHITE)
        }
        musicPill.addView(musicPillText)
        lockOverlay.addView(musicPill)

        // Bottom Swipe Bar
        val swipeBar = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(90), dp(4)).apply {
                setMargins(0, dp(16), 0, 0)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B3FFFFFF"))
                cornerRadius = dp(2).toFloat()
            }
        }
        lockOverlay.addView(swipeBar)

        screenArea.addView(lockOverlay)
        phoneFrame.addView(screenArea)
        root.addView(phoneFrame)

        // Bottom Navigation Bar
        val bottomBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC18181B"))
                cornerRadius = dp(24).toFloat()
            }
            layoutParams = FrameLayout.LayoutParams(wrap, wrap).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setMargins(0, 0, 0, dp(24))
            }
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        val prevBtn = Button(context).apply {
            text = "← Précédent"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                if (project.slides.isNotEmpty()) {
                    currentIndex = (currentIndex - 1 + project.slides.size) % project.slides.size
                    updateSlideView()
                }
            }
        }
        bottomBar.addView(prevBtn)

        val nextBtn = Button(context).apply {
            text = "Suivant →"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                if (project.slides.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % project.slides.size
                    updateSlideView()
                }
            }
        }
        bottomBar.addView(nextBtn)
        root.addView(bottomBar)

        setContentView(root)
        updateSlideView()
        handler.post(timerRunnable)
    }

    private fun updateSlideView() {
        if (project.slides.isEmpty()) return

        val slide = project.slides[currentIndex]
        countdownSec = slide.durationSec

        // Image
        val f = File(Slides.slidesDir(context), slide.fileName)
        if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            if (bmp != null) {
                wallpaperImage.setImageBitmap(Slides.processFilteredBitmap(bmp, slide.adjustments))
            }
        }

        // Music Pill & Playback
        val audio = slide.audio
        if (audio != null) {
            musicPill.visibility = View.VISIBLE
            musicPillText.text = audio.name
            startSlideAudio(audio)
        } else {
            musicPill.visibility = View.GONE
            stopAudio()
        }
    }

    private fun startSlideAudio(clip: AudioClip) {
        stopAudio()
        if (isMuted) return

        val fileName = clip.fileName ?: return
        val audioFile = File(Slides.audioDir(context), fileName)
        if (!audioFile.exists()) return

        try {
            player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setVolume(clip.volume, clip.volume)
                isLooping = true
                prepare()
                if (clip.startTimeSec > 0) {
                    seekTo(clip.startTimeSec * 1000)
                }
                start()
            }
        } catch (e: Exception) {}
    }

    private fun stopAudio() {
        player?.stop()
        player?.release()
        player = null
    }

    override fun onStop() {
        handler.removeCallbacks(timerRunnable)
        stopAudio()
        super.onStop()
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
}
