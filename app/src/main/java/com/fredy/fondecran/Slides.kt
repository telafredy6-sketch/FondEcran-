package com.fredy.fondecran

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object Slides {

    fun prefs(ctx: Context) = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun dir(ctx: Context): File = File(ctx.filesDir, "slides").apply { mkdirs() }

    fun files(ctx: Context): List<File> =
        dir(ctx).listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()

    // ---------- musique associée à chaque image ----------

    fun musicOf(ctx: Context, img: File) =
        File(dir(ctx), img.nameWithoutExtension + ".audio")

    fun musicName(ctx: Context, img: File): String? =
        if (musicOf(ctx, img).exists())
            prefs(ctx).getString("mname_" + img.nameWithoutExtension, "musique")
        else null

    fun importMusic(ctx: Context, img: File, uri: Uri) {
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(musicOf(ctx, img)).use { out -> input.copyTo(out) }
        }
        prefs(ctx).edit()
            .putString("mname_" + img.nameWithoutExtension, displayName(ctx, uri))
            .apply()
    }

    fun removeMusic(ctx: Context, img: File) {
        musicOf(ctx, img).delete()
        prefs(ctx).edit().remove("mname_" + img.nameWithoutExtension).apply()
    }

    private fun displayName(ctx: Context, uri: Uri): String {
        try {
            ctx.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        } catch (e: Exception) {
            // on garde le nom par défaut
        }
        return uri.lastPathSegment ?: "musique"
    }

    // ---------- images ----------

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
        val p = prefs(ctx)
        val e = p.edit()
        p.all.keys.filter { it.startsWith("mname_") }.forEach { e.remove(it) }
        e.putInt("idx", 0).apply()
        return n
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
