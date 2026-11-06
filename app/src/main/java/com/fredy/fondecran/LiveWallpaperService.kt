package com.fredy.fondecran

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
        private var currentFile: File? = null
        private var index = 0
        private var w = 0
        private var h = 0
        private var isShown = false

        private val tick = object : Runnable {
            override fun run() {
                nextImage()
                handler.postDelayed(this, intervalMs())
            }
        }

        private fun intervalMs(): Long =
            Slides.prefs(applicationContext).getInt("interval_min", 15) * 60_000L

        private fun nextImage() {
            val list = Slides.files(applicationContext)
            if (list.isEmpty()) {
                bmp = null
                currentFile = null
            } else {
                index %= list.size
                currentFile = list[index]
                bmp = BitmapFactory.decodeFile(list[index].absolutePath)
                index++
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
                bmp?.let { canvas.drawBitmap(it, null, Rect(0, 0, w, h), null) }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        /** Prépare la musique associée à l'image affichée. */
        private fun prepareMusic() {
            player?.release()
            player = null
            val img = currentFile ?: return
            if (!Slides.prefs(applicationContext).getBoolean("music_on", true)) return
            val m = Slides.musicOf(applicationContext, img)
            if (!m.exists()) return
            try {
                player = MediaPlayer().apply {
                    setDataSource(m.absolutePath)
                    isLooping = true
                    prepare()
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
                handler.postDelayed(tick, intervalMs())
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
