package com.fredy.fondecran

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object Slides {

    fun prefs(ctx: Context) = ctx.getSharedPreferences("fond_ecran_cfg_v2", Context.MODE_PRIVATE)

    fun slidesDir(ctx: Context): File = File(ctx.filesDir, "slides_v2").apply { mkdirs() }

    fun audioDir(ctx: Context): File = File(ctx.filesDir, "audio_v2").apply { mkdirs() }

    // ---------- Gestion des projets persistants ----------

    fun loadProjects(ctx: Context): MutableList<SlideshowProject> {
        val raw = prefs(ctx).getString("projects_json", null)
        val list = mutableListOf<SlideshowProject>()
        if (raw != null) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    list.add(SlideshowProject.fromJson(arr.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Si aucun projet n'existe, on initialise le projet starter identique au TypeScript
        if (list.isEmpty()) {
            val starterSlides = Presets.createDefaultStarterWallpapers(ctx)
            val defaultProject = SlideshowProject(
                id = "proj-1",
                title = "Mon Diaporama Nature & Anime",
                description = "Diaporama animé avec musiques ambiantes",
                mode = "slideshow",
                globalTarget = 2,
                defaultDurationSec = 10
            ).apply {
                slides.addAll(starterSlides)
            }
            list.add(defaultProject)
            saveProjects(ctx, list)
        }
        return list
    }

    fun saveProjects(ctx: Context, projects: List<SlideshowProject>) {
        val arr = JSONArray()
        projects.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("projects_json", arr.toString()).apply()
    }

    fun getActiveProject(ctx: Context): SlideshowProject {
        val projects = loadProjects(ctx)
        val activeId = prefs(ctx).getString("active_project_id", null)
        return projects.find { it.id == activeId } ?: projects.first()
    }

    fun setActiveProject(ctx: Context, project: SlideshowProject) {
        prefs(ctx).edit().putString("active_project_id", project.id).apply()
    }

    // ---------- Configuration Globale ----------

    fun loadAppConfig(ctx: Context): AppConfig {
        val p = prefs(ctx)
        return AppConfig(
            appWallpaperFileName = p.getString("cfg_app_wallpaper", null),
            appWallpaperBlur = p.getInt("cfg_app_blur", 10),
            appWallpaperOpacity = p.getInt("cfg_app_opacity", 65),
            securityPinEnabled = p.getBoolean("cfg_security_enabled", false),
            securityPin = p.getString("cfg_security_pin", "1234") ?: "1234",
            exportQuality = p.getString("cfg_export_quality", "high") ?: "high"
        )
    }

    fun saveAppConfig(ctx: Context, config: AppConfig) {
        prefs(ctx).edit()
            .putString("cfg_app_wallpaper", config.appWallpaperFileName)
            .putInt("cfg_app_blur", config.appWallpaperBlur)
            .putInt("cfg_app_opacity", config.appWallpaperOpacity)
            .putBoolean("cfg_security_enabled", config.securityPinEnabled)
            .putString("cfg_security_pin", config.securityPin)
            .putString("cfg_export_quality", config.exportQuality)
            .apply()
    }

    // ---------- Importation & Recadrage (CenterCrop) ----------

    fun importImage(ctx: Context, uri: Uri): SlideItem? {
        val d = slidesDir(ctx)
        val id = System.currentTimeMillis().toString() + "_" + (0..999).random()
        val fileName = "$id.jpg"
        val destFile = File(d, fileName)

        try {
            val bmp = decode(ctx, uri) ?: return null
            val dm = ctx.resources.displayMetrics
            val cropped = centerCrop(bmp, dm.widthPixels, dm.heightPixels)
            FileOutputStream(destFile).use {
                cropped.compress(Bitmap.CompressFormat.JPEG, 92, it)
            }
            val displayName = getDisplayName(ctx, uri) ?: "Image"
            return SlideItem(
                id = id,
                name = displayName,
                fileName = fileName,
                durationSec = 15
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun importAudio(ctx: Context, uri: Uri): Pair<String, String>? {
        val d = audioDir(ctx)
        val id = "audio_" + System.currentTimeMillis()
        val dest = File(d, id)
        try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            }
            val name = getDisplayName(ctx, uri) ?: "Musique"
            return Pair(id, name)
        } catch (e: Exception) {
            return null
        }
    }

    fun getDisplayName(ctx: Context, uri: Uri): String? {
        try {
            ctx.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        } catch (e: Exception) {}
        return uri.lastPathSegment
    }

    private fun decode(ctx: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 3000) sample *= 2
        val real = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, real) }
    }

    fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
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

    // ---------- Traitement d'Image : Zoom, Pan & ColorMatrix Filters ----------

    fun processFilteredBitmap(baseBitmap: Bitmap, adjustments: ImageAdjustments): Bitmap {
        val w = baseBitmap.width
        val h = baseBitmap.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Fond d'écran coloré derrière
        try {
            canvas.drawColor(Color.parseColor(adjustments.bgColor))
        } catch (e: Exception) {
            canvas.drawColor(Color.BLACK)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val cm = ColorMatrix()

        // 1. Saturation
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(adjustments.saturation / 100f)

        // 2. Contraste & Luminosité
        val scale = adjustments.contrast / 100f
        val translate = (adjustments.brightness - 100) * 1.28f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        cm.postConcat(satMatrix)
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)

        // Transformation Matrix (Zoom & Pan)
        val matrix = Matrix()
        val z = adjustments.zoom
        matrix.postScale(z, z, w / 2f, h / 2f)
        matrix.postTranslate(adjustments.panX, adjustments.panY)

        canvas.drawBitmap(baseBitmap, matrix, paint)
        return result
    }

    // ---------- Application du Fond d'Écran Android Réel ----------

    fun applyNext(ctx: Context): Boolean {
        val project = getActiveProject(ctx)
        if (project.slides.isEmpty()) return false

        project.currentSlideIndex = (project.currentSlideIndex + 1) % project.slides.size
        val currentSlide = project.slides[project.currentSlideIndex]

        val file = File(slidesDir(ctx), currentSlide.fileName)
        if (!file.exists()) return false

        val baseBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        val finalBitmap = processFilteredBitmap(baseBitmap, currentSlide.adjustments)

        val flag = when (project.globalTarget) {
            0 -> WallpaperManager.FLAG_SYSTEM
            1 -> WallpaperManager.FLAG_LOCK
            else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }

        try {
            WallpaperManager.getInstance(ctx).setBitmap(finalBitmap, null, true, flag)
            // Mise à jour de l'index dans les projets
            val projects = loadProjects(ctx)
            val pIdx = projects.indexOfFirst { it.id == project.id }
            if (pIdx >= 0) {
                projects[pIdx] = project
                saveProjects(ctx, projects)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // ---------- Arrière-Plan Interne de l'Application ----------

    fun setAppWallpaper(ctx: Context, fileName: String?) {
        val config = loadAppConfig(ctx)
        config.appWallpaperFileName = fileName
        saveAppConfig(ctx, config)
    }

    fun applyAppWallpaper(view: View, ctx: Context) {
        val config = loadAppConfig(ctx)
        val fileName = config.appWallpaperFileName ?: return
        val file = File(slidesDir(ctx), fileName)
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                view.background = BitmapDrawable(ctx.resources, bmp).apply {
                    alpha = ((config.appWallpaperOpacity / 100f) * 255).toInt()
                }
            }
        }
    }
}
