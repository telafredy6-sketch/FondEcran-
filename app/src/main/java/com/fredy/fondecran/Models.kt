package com.fredy.fondecran

import org.json.JSONArray
import org.json.JSONObject

// Target pour le fond d'écran : 0 = Accueil, 1 = Verrouillage, 2 = Les deux
typealias WallpaperTarget = Int

data class ImageAdjustments(
    var zoom: Float = 1.0f,
    var panX: Float = 0f,
    var panY: Float = 0f,
    var aspectRatio: String = "9:16", // "9:16", "1:1", "16:9", "free"
    var brightness: Int = 100, // 50 to 150 (100 = normal)
    var contrast: Int = 100,   // 50 to 150 (100 = normal)
    var saturation: Int = 100, // 0 to 200 (100 = normal)
    var hueRotate: Int = 0,    // 0 to 360
    var blur: Int = 0,         // 0 to 20 px
    var filterPreset: String = "normal", // normal, vivid, bw, vintage, warm, cyberpunk, cinematic
    var bgColor: String = "#000000"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("zoom", zoom.toDouble())
        put("panX", panX.toDouble())
        put("panY", panY.toDouble())
        put("aspectRatio", aspectRatio)
        put("brightness", brightness)
        put("contrast", contrast)
        put("saturation", saturation)
        put("hueRotate", hueRotate)
        put("blur", blur)
        put("filterPreset", filterPreset)
        put("bgColor", bgColor)
    }

    companion object {
        fun fromJson(json: JSONObject?): ImageAdjustments {
            if (json == null) return ImageAdjustments()
            return ImageAdjustments(
                zoom = json.optDouble("zoom", 1.0).toFloat(),
                panX = json.optDouble("panX", 0.0).toFloat(),
                panY = json.optDouble("panY", 0.0).toFloat(),
                aspectRatio = json.optString("aspectRatio", "9:16"),
                brightness = json.optInt("brightness", 100),
                contrast = json.optInt("contrast", 100),
                saturation = json.optInt("saturation", 100),
                hueRotate = json.optInt("hueRotate", 0),
                blur = json.optInt("blur", 0),
                filterPreset = json.optString("filterPreset", "normal"),
                bgColor = json.optString("bgColor", "#000000")
            )
        }
    }
}

data class AudioClip(
    val id: String = System.currentTimeMillis().toString(),
    var name: String = "Ambiance Sonore",
    var fileName: String? = null,
    var durationSec: Int = 60,
    var startTimeSec: Int = 0,
    var endTimeSec: Int = 30,
    var volume: Float = 0.8f,
    var fadeIn: Boolean = true,
    var fadeOut: Boolean = true,
    var isSynth: Boolean = true,
    var synthType: String = "lofi" // "lofi", "ambient", "nature", "chill"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("fileName", fileName ?: "")
        put("durationSec", durationSec)
        put("startTimeSec", startTimeSec)
        put("endTimeSec", endTimeSec)
        put("volume", volume.toDouble())
        put("fadeIn", fadeIn)
        put("fadeOut", fadeOut)
        put("isSynth", isSynth)
        put("synthType", synthType)
    }

    companion object {
        fun fromJson(json: JSONObject?): AudioClip? {
            if (json == null) return null
            return AudioClip(
                id = json.optString("id", System.currentTimeMillis().toString()),
                name = json.optString("name", "Ambiance"),
                fileName = json.optString("fileName").takeIf { it.isNotBlank() },
                durationSec = json.optInt("durationSec", 60),
                startTimeSec = json.optInt("startTimeSec", 0),
                endTimeSec = json.optInt("endTimeSec", 30),
                volume = json.optDouble("volume", 0.8).toFloat(),
                fadeIn = json.optBoolean("fadeIn", true),
                fadeOut = json.optBoolean("fadeOut", true),
                isSynth = json.optBoolean("isSynth", true),
                synthType = json.optString("synthType", "lofi")
            )
        }
    }
}

data class SlideItem(
    val id: String = System.currentTimeMillis().toString(),
    var name: String,
    val fileName: String,
    var durationSec: Int = 15, // Durée individuelle par image
    var adjustments: ImageAdjustments = ImageAdjustments(),
    var audio: AudioClip? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("fileName", fileName)
        put("durationSec", durationSec)
        put("adjustments", adjustments.toJson())
        if (audio != null) put("audio", audio!!.toJson())
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): SlideItem = SlideItem(
            id = json.optString("id", System.currentTimeMillis().toString()),
            name = json.optString("name", "Image"),
            fileName = json.optString("fileName", ""),
            durationSec = json.optInt("durationSec", 15),
            adjustments = ImageAdjustments.fromJson(json.optJSONObject("adjustments")),
            audio = AudioClip.fromJson(json.optJSONObject("audio")),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

data class SlideshowProject(
    val id: String = "proj_" + System.currentTimeMillis(),
    var title: String = "Nouveau Projet",
    var description: String = "",
    var mode: String = "slideshow", // "slideshow" ou "single"
    var globalTarget: WallpaperTarget = 2, // 0 = Accueil, 1 = Verrouillage, 2 = Les deux
    var defaultDurationSec: Int = 15,
    val slides: MutableList<SlideItem> = mutableListOf(),
    var currentSlideIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("description", description)
        put("mode", mode)
        put("globalTarget", globalTarget)
        put("defaultDurationSec", defaultDurationSec)
        put("currentSlideIndex", currentSlideIndex)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        val arr = JSONArray()
        slides.forEach { arr.put(it.toJson()) }
        put("slides", arr)
    }

    companion object {
        fun fromJson(json: JSONObject): SlideshowProject {
            val proj = SlideshowProject(
                id = json.optString("id", System.currentTimeMillis().toString()),
                title = json.optString("title", "Projet"),
                description = json.optString("description", ""),
                mode = json.optString("mode", "slideshow"),
                globalTarget = json.optInt("globalTarget", 2),
                defaultDurationSec = json.optInt("defaultDurationSec", 15),
                currentSlideIndex = json.optInt("currentSlideIndex", 0),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
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

data class PinterestItem(
    val id: String,
    val title: String,
    val category: String, // nature, ocean, anime, cyberpunk, minimal, space
    val author: String,
    val pinUrl: String,
    val imageUrl: String,
    val tags: List<String>
)

data class SystemSound(
    val id: String,
    val title: String,
    val category: String, // ringtone, notification, alarm
    val durationLabel: String
)

data class AppConfig(
    var appWallpaperFileName: String? = null,
    var appWallpaperBlur: Int = 10,
    var appWallpaperOpacity: Int = 65,
    var securityPinEnabled: Boolean = false,
    var securityPin: String = "1234",
    var exportQuality: String = "high"
)
