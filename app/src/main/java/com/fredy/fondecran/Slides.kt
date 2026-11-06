package com.fredy.fondecran

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Slides {

    fun prefs(ctx: Context) = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun dir(ctx: Context): File = File(ctx.filesDir, "slides").apply { mkdirs() }

    fun files(ctx: Context): List<File> =
        dir(ctx).listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()

    fun musicFile(ctx: Context) = File(ctx.filesDir, "music.audio")

    /** Copie les images choisies dans le stockage de l'app, recadrées au format de l'écran. */
    fun importImages(ctx: Context, uris: List<Uri>): Int {
        val d = dir(ctx)
        d.listFiles()?.forEach { it.delete() }
        val dm = ctx.resources.displayMetrics
        var n = 0
        for (u in uris) {
            try {
                val bmp = decode(ctx, u) ?: continue
                val cropped = centerCrop(bmp, dm.widthPixels, dm.heightPixels)
                FileOutputStream(File(d, "%03d.jpg".format(n))).use {
                    cropped.compress(Bitmap.CompressFormat.JPEG, 90, it)
                }
                n++
            } catch (e: Exception) {
                // image ignorée
            }
        }
        prefs(ctx).edit().putInt("idx", 0).apply()
        return n
    }

    fun importMusic(ctx: Context, uri: Uri) {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(musicFile(ctx)).use { out -> input.copyTo(out) }
        }
        prefs(ctx).edit().putString("music_name", uri.lastPathSegment ?: "musique").apply()
    }

    /** Applique l'image suivante comme vrai fond d'écran. */
    fun applyNext(ctx: Context): Boolean {
        val list = files(ctx)
        if (list.isEmpty()) return false
        val p = prefs(ctx)
        val idx = p.getInt("idx", 0) % list.size
        val bmp = BitmapFactory.decodeFile(list[idx].absolutePath) ?: return false
        val flag = when (p.getInt("target", 2)) {
            0 -> WallpaperManager.FLAG_SYSTEM
            1 -> WallpaperManager.FLAG_LOCK
            else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        WallpaperManager.getInstance(ctx).setBitmap(bmp, null, true, flag)
        p.edit().putInt("idx", idx + 1).apply()
        return true
    }

    private fun decode(ctx: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 3000) sample *= 2
        val real = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, real) }
    }

    private fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val targetRatio = w.toFloat() / h
        val srcRatio = src.width.toFloat() / src.height
        return if (srcRatio > targetRatio) {
            val newW = (src.height * targetRatio).toInt()
            Bitmap.createBitmap(src, (src.width - newW) / 2, 0, newW, src.height)
        } else {
            val newH = (src.width / targetRatio).toInt()
            Bitmap.createBitmap(src, 0, (src.height - newH) / 2, src.width, newH)
        }
    }
}
