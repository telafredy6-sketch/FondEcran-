package com.fredy.fondecran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import java.io.File
import java.io.FileOutputStream

object Presets {

    val PINTEREST_EXPLORE_FEED = listOf(
        PinterestItem(
            id = "pin-1",
            title = "Cascade Émeraude",
            category = "nature",
            author = "@natural_wanderlust",
            pinUrl = "https://www.pinterest.com/search/pins/?q=emerald%20waterfall%20wallpaper",
            imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1080&q=85",
            tags = listOf("Nature", "Cascade", "Forêt")
        ),
        PinterestItem(
            id = "pin-2",
            title = "Vague Océanique Crépuscule",
            category = "ocean",
            author = "@ocean_vibes",
            pinUrl = "https://www.pinterest.com/search/pins/?q=sunset%20ocean%20waves%20wallpaper",
            imageUrl = "https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=1080&q=85",
            tags = listOf("Océan", "Vagues", "Coucher de soleil")
        ),
        PinterestItem(
            id = "pin-3",
            title = "Ciel Étoilé Anime Néo-Tokyo",
            category = "anime",
            author = "@aesthetic_anime_jp",
            pinUrl = "https://www.pinterest.com/search/pins/?q=anime%20night%20sky%20wallpaper",
            imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1080&q=85",
            tags = listOf("Anime", "Nuit", "Étoiles")
        ),
        PinterestItem(
            id = "pin-4",
            title = "Cyberpunk Neon Rain",
            category = "cyberpunk",
            author = "@future_synth",
            pinUrl = "https://www.pinterest.com/search/pins/?q=cyberpunk%20neon%20rain%20wallpaper",
            imageUrl = "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=1080&q=85",
            tags = listOf("Cyberpunk", "Néon", "Pluie")
        ),
        PinterestItem(
            id = "pin-5",
            title = "Minimalisme Courbes Pastel",
            category = "minimal",
            author = "@minimal_design",
            pinUrl = "https://www.pinterest.com/search/pins/?q=minimalist%20pastel%20wallpaper",
            imageUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=1080&q=85",
            tags = listOf("Minimaliste", "Pastel", "Abstrait")
        ),
        PinterestItem(
            id = "pin-6",
            title = "Nébuleuse Cosmique Violette",
            category = "space",
            author = "@deep_space",
            pinUrl = "https://www.pinterest.com/search/pins/?q=space%20nebula%20purple%20wallpaper",
            imageUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?auto=format&fit=crop&w=1080&q=85",
            tags = listOf("Espace", "Galaxie", "Cosmos")
        )
    )

    val SYSTEM_SOUNDS_CATALOG = listOf(
        SystemSound("notif-1", "Goutte d’eau pure", "notification", "0:01"),
        SystemSound("notif-2", "Carillon zen", "notification", "0:02"),
        SystemSound("ring-1", "Mélodie Bleue", "ringtone", "0:04"),
        SystemSound("ring-2", "Marimba Doux", "ringtone", "0:03"),
        SystemSound("alarm-1", "Réveil Lumineux", "alarm", "0:03")
    )

    val AUDIO_AMBIENT_PRESETS = listOf(
        Pair("Lofi Chill Chords", "lofi"),
        Pair("Océan Méditation", "ambient"),
        Pair("Brise Naturelle", "nature"),
        Pair("Mélodie Crépuscule", "chill")
    )

    /**
     * Génère les fonds d'écran de démarrage avec gradients haute définition (1080x1920)
     * identiques à la version TypeScript !
     */
    fun createDefaultStarterWallpapers(ctx: Context): List<SlideItem> {
        val dir = Slides.slidesDir(ctx)
        val w = 1080
        val h = 1920
        val starters = listOf(
            Triple("Nature Horizon", Color.parseColor("#81D4FA"), Color.parseColor("#FFF9C4")),
            Triple("Océan Pacifique", Color.parseColor("#4DD0E1"), Color.parseColor("#01579B")),
            Triple("Montagnes Éternelles", Color.parseColor("#7986CB"), Color.parseColor("#1A237E")),
            Triple("Anime Crépuscule", Color.parseColor("#E1BEE7"), Color.parseColor("#311B92"))
        )

        val result = mutableListOf<SlideItem>()
        starters.forEachIndexed { idx, item ->
            val fileName = "starter_${idx + 1}.jpg"
            val file = File(dir, fileName)
            if (!file.exists()) {
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                // Gradient background
                paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), item.second, item.third, Shader.TileMode.CLAMP)
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                paint.shader = null

                // Stylized decorative shapes
                paint.color = Color.WHITE
                paint.alpha = 50
                canvas.drawCircle(w / 2f, h * 0.45f, 280f, paint)

                paint.alpha = 180
                val path = Path().apply {
                    moveTo(0f, h * 0.75f)
                    quadTo(w * 0.5f, h * 0.65f, w.toFloat(), h * 0.75f)
                    lineTo(w.toFloat(), h.toFloat())
                    lineTo(0f, h.toFloat())
                    close()
                }
                paint.color = item.third
                canvas.drawPath(path, paint)

                FileOutputStream(file).use {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, it)
                }
            }

            result.add(
                SlideItem(
                    id = "starter-$idx",
                    name = item.first,
                    fileName = fileName,
                    durationSec = if (idx == 0) 5 else if (idx == 1) 10 else 15,
                    audio = AudioClip(
                        name = AUDIO_AMBIENT_PRESETS[idx % AUDIO_AMBIENT_PRESETS.size].first,
                        synthType = AUDIO_AMBIENT_PRESETS[idx % AUDIO_AMBIENT_PRESETS.size].second,
                        isSynth = true
                    )
                )
            )
        }
        return result
    }
}
