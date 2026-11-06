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

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SlideEngine()

    inner class SlideEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var player: MediaPlayer? = null
        private var musicStamp = 0L
        private var bmp: Bitmap? = null
        private var index = 0
        private var w = 0
        private var h = 0

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
            } else {
                index %= list.size
                bmp = BitmapFactory.decodeFile(list[index].absolutePath)
                index++
            }
            draw()
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

        private fun startMusic() {
            if (!Slides.prefs(applicationContext).getBoolean("music_on", true)) return
            val f = Slides.musicFile(applicationContext)
            if (!f.exists()) return
            try {
                if (player == null || musicStamp != f.lastModified()) {
                    player?.release()
                    musicStamp = f.lastModified()
                    player = MediaPlayer().apply {
                        setDataSource(f.absolutePath)
                        isLooping = true
                        prepare()
                    }
                }
                player?.start()
            } catch (e: Exception) {
                player?.release()
                player = null
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
            handler.removeCallbacks(tick)
            if (visible) {
                if (bmp == null) nextImage() else draw()
                handler.postDelayed(tick, intervalMs())
                startMusic()
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
