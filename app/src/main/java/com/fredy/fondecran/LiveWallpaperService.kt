package com.fredy.fondecran

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SlideEngine()

    inner class SlideEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var player: MediaPlayer? = null
        private var bmp: Bitmap? = null
        private var currentSlide: SlideItem? = null
        private var isShown = false
        private var w = 0
        private var h = 0

        private val tick = object : Runnable {
            override fun run() {
                nextImage()
                val delay = (currentSlide?.durationSec ?: 15) * 1000L
                handler.postDelayed(this, delay)
            }
        }

        private fun nextImage() {
            val project = Slides.getActiveProject(applicationContext)
            val list = project.slides
            if (list.isEmpty()) {
                bmp = null
                currentSlide = null
            } else {
                project.currentSlideIndex = (project.currentSlideIndex + 1) % list.size
                currentSlide = list[project.currentSlideIndex]
                val f = File(Slides.slidesDir(applicationContext), currentSlide!!.fileName)
                if (f.exists()) {
                    val raw = BitmapFactory.decodeFile(f.absolutePath)
                    bmp = if (raw != null) Slides.processFilteredBitmap(raw, currentSlide!!) else null
                } else {
                    bmp = null
                }
            }
            draw()
            prepareMusic()
            if (isShown) playMusic()
        }

        private fun draw() {
            if (w == 0 || h == 0) return
            val holder = surfaceHolder
            val canvas = try {
                holder.lockCanvas()
            } catch (e: Exception) {
                null
            } ?: return

            try {
                canvas.drawColor(Color.BLACK)
                val currentBmp = bmp
                if (currentBmp != null) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    val slide = currentSlide
                    val zoom = slide?.zoom ?: 1.0f
                    val panX = slide?.panX ?: 0f
                    val panY = slide?.panY ?: 0f

                    val matrix = Matrix()
                    // Calcul d'échelle pour remplir l'écran (CENTER_CROP)
                    val scale = maxOf(w.toFloat() / currentBmp.width, h.toFloat() / currentBmp.height) * zoom
                    val dx = (w - currentBmp.width * scale) / 2f + panX
                    val dy = (h - currentBmp.height * scale) / 2f + panY
                    matrix.postScale(scale, scale)
                    matrix.postTranslate(dx, dy)

                    canvas.drawBitmap(currentBmp, matrix, paint)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        /** Prépare la musique associée à la diapositive active */
        private fun prepareMusic() {
            player?.release()
            player = null

            val slide = currentSlide ?: return
            val audioName = slide.audioFileName ?: return
            val audioFile = File(Slides.audioDir(applicationContext), audioName)
            if (!audioFile.exists()) return

            val musicOn = Slides.prefs(applicationContext).getBoolean("music_on", true)
            if (!musicOn) return

            try {
                player = MediaPlayer().apply {
                    setDataSource(audioFile.absolutePath)
                    setVolume(slide.audioVolume, slide.audioVolume)
                    isLooping = true
                    prepare()
                    if (slide.audioStartMs > 0) {
                        seekTo(slide.audioStartMs)
                    }
                }
            } catch (e: Exception) {
                player?.release()
                player = null
            }
        }

        private fun playMusic() {
            try {
                player?.start()
            } catch (e: Exception) {
            }
        }

        private fun pauseMusic() {
            try {
                player?.pause()
            } catch (e: Exception) {
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            w = width
            h = height
            if (bmp == null) nextImage() else draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isShown = visible
            handler.removeCallbacks(tick)
            if (visible) {
                if (bmp == null) nextImage() else draw()
                if (player == null) prepareMusic()
                playMusic()
                val delay = (currentSlide?.durationSec ?: 15) * 1000L
                handler.postDelayed(tick, delay)
            } else {
                pauseMusic()
            }
        }

        override fun onDestroy() {
            handler.removeCallbacks(tick)
            player?.release()
            player = null
            super.onDestroy()
        }
    }
}
