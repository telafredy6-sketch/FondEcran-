package com.fredy.fondecran

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class SlideItem(
    val id: String,
    var name: String,
    val fileName: String,
    var durationSec: Int = 15, // Durée individuelle par image !
    var audioName: String? = null,
    var audioFileName: String? = null,
    var audioStartMs: Int = 0,
    var audioEndMs: Int = 30000,
    var audioVolume: Float = 0.8f,
    var zoom: Float = 1.0f,
    var panX: Float = 0f,
    var panY: Float = 0f,
    var brightness: Int = 100, // 50 to 150
    var contrast: Int = 100,   // 50 to 150
    var saturation: Int = 100, // 0 to 200
    var filterPreset: String = "normal"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("fileName", fileName)
        put("durationSec", durationSec)
        put("audioName", audioName ?: "")
        put("audioFileName", audioFileName ?: "")
        put("audioStartMs", audioStartMs)
        put("audioEndMs", audioEndMs)
        put("audioVolume", audioVolume.toDouble())
        put("zoom", zoom.toDouble())
        put("panX", panX.toDouble())
        put("panY", panY.toDouble())
        put("brightness", brightness)
        put("contrast", contrast)
        put("saturation", saturation)
        put("filterPreset", filterPreset)
    }

    companion object {
        fun fromJson(json: JSONObject): SlideItem = SlideItem(
            id = json.optString("id", System.currentTimeMillis().toString()),
            name = json.optString("name", "Image"),
            fileName = json.optString("fileName", ""),
            durationSec = json.optInt("durationSec", 15),
            audioName = json.optString("audioName").takeIf { it.isNotBlank() },
            audioFileName = json.optString("audioFileName").takeIf { it.isNotBlank() },
            audioStartMs = json.optInt("audioStartMs", 0),
            audioEndMs = json.optInt("audioEndMs", 30000),
            audioVolume = json.optDouble("audioVolume", 0.8).toFloat(),
            zoom = json.optDouble("zoom", 1.0).toFloat(),
            panX = json.optDouble("panX", 0.0).toFloat(),
            panY = json.optDouble("panY", 0.0).toFloat(),
            brightness = json.optInt("brightness", 100),
            contrast = json.optInt("contrast", 100),
            saturation = json.optInt("saturation", 100),
            filterPreset = json.optString("filterPreset", "normal")
        )
    }
}

data class SlideshowProject(
    val id: String,
    var title: String,
    var mode: String = "slideshow", // "slideshow" ou "single"
    var target: Int = 2, // 0: Accueil, 1: Verrouillage, 2: Les deux
    var defaultDurationSec: Int = 15,
    val slides: MutableList<SlideItem> = mutableListOf(),
    var currentSlideIndex: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("mode", mode)
        put("target", target)
        put("defaultDurationSec", defaultDurationSec)
        put("currentSlideIndex", currentSlideIndex)
        val arr = JSONArray()
        slides.forEach { arr.put(it.toJson()) }
        put("slides", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): SlideshowProject {
            val proj = SlideshowProject(
                id = json.optString("id", System.currentTimeMillis().toString()),
                title = json.optString("title", "Nouveau Diaporama"),
                mode = json.optString("mode", "slideshow"),
                target = json.optInt("target", 2),
                defaultDurationSec = json.optInt("defaultDurationSec", 15),
                currentSlideIndex = json.optInt("currentSlideIndex", 0)
            )
            val arr = json.optJSONArray("slides")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    proj.slides.add(SlideItem.fromJson(arr.getJSONObject(i)))
                }
            }
            return proj
        }
    }
}

object Slides {

    fun prefs(ctx: Context) = ctx.getSharedPreferences("fond_ecran_cfg_v2", Context.MODE_PRIVATE)

    fun slidesDir(ctx: Context): File = File(ctx.filesDir, "slides_v2").apply { mkdirs() }

    fun audioDir(ctx: Context): File = File(ctx.filesDir, "audio_v2").apply { mkdirs() }

    // ---------- Gestion des projets persistants (Room-like JSON) ----------

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
        if (list.isEmpty()) {
            val defaultProject = SlideshowProject(
                id = "proj-default",
                title = "Mon Premier Diaporama",
                mode = "slideshow",
                defaultDurationSec = 15
            )
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

    // ---------- Importation & Recadrage d'images (centerCrop) ----------

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
        } catch (e: Exception) {
        }
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

    // ---------- Traitement d'image & Filtres (Luminosité, Contraste, Saturation) ----------

    fun processFilteredBitmap(baseBitmap: Bitmap, slide: SlideItem): Bitmap {
        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix()

        // Saturation (0 to 2)
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(slide.saturation / 100f)

        // Contraste & Luminosité
        val scale = slide.contrast / 100f
        val translate = (slide.brightness - 100) * 1.28f
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

        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)
        return result
    }

    // ---------- Application du vrai fond d'écran Android ----------

    fun applyNext(ctx: Context): Boolean {
        val project = getActiveProject(ctx)
        if (project.slides.isEmpty()) return false

        project.currentSlideIndex = (project.currentSlideIndex + 1) % project.slides.size
        val currentSlide = project.slides[project.currentSlideIndex]

        val file = File(slidesDir(ctx), currentSlide.fileName)
        if (!file.exists()) return false

        val baseBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
        val finalBitmap = processFilteredBitmap(baseBitmap, currentSlide)

        val flag = when (project.target) {
            0 -> WallpaperManager.FLAG_SYSTEM
            1 -> WallpaperManager.FLAG_LOCK
            else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }

        try {
            WallpaperManager.getInstance(ctx).setBitmap(finalBitmap, null, true, flag)
            // Sauvegarder l'état
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

    // ---------- Fond d'écran interne de l'Application elle-même ----------

    fun setAppWallpaperPath(ctx: Context, path: String?) {
        prefs(ctx).edit().putString("app_wallpaper_path", path).apply()
    }

    fun getAppWallpaperPath(ctx: Context): String? {
        return prefs(ctx).getString("app_wallpaper_path", null)
    }

    fun applyAppWallpaper(view: View, ctx: Context) {
        val path = getAppWallpaperPath(ctx) ?: return
        val file = File(path)
        if (file.exists()) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                view.background = BitmapDrawable(ctx.resources, bmp)
            }
        }
    }
}
