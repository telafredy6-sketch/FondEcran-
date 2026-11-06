package com.fredy.fondecran

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*

class SecurityGateDialog(
    context: Context,
    private val correctPin: String,
    private val onUnlock: () -> Unit
) : Dialog(context) {

    private val match = ViewGroup.LayoutParams.MATCH_PARENT
    private val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

    private val mainBlue = Color.parseColor("#1E88E5")
    private val darkBlue = Color.parseColor("#0D47A1")

    private var enteredPin = ""
    private val dots = mutableListOf<View>()
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#F5E3F2FD")))
        setCancelable(false)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(match, match)
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }

        // Lock Icon
        val icon = TextView(context).apply {
            text = "🔒"
            textSize = 36f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                setMargins(0, 0, 0, dp(12))
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#BBDEFB"))
                cornerRadius = dp(36).toFloat()
            }
        }
        root.addView(icon)

        root.addView(TextView(context).apply {
            text = "Fond d'écran"
            textSize = 24f
            setTextColor(darkBlue)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(context).apply {
            text = "Confirme ton identité pour ouvrir l'app"
            textSize = 14f
            setTextColor(Color.parseColor("#1565C0"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(24))
        })

        // PIN Dots
        val dotsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(match, wrap).apply { setMargins(0, 0, 0, dp(16)) }
        }
        for (i in 0 until 4) {
            val dot = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply {
                    setMargins(dp(8), 0, dp(8), 0)
                }
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(2), mainBlue)
                }
            }
            dots.add(dot)
            dotsRow.addView(dot)
        }
        root.addView(dotsRow)

        errorText = TextView(context).apply {
            text = "Code incorrect (Code par défaut : $correctPin)"
            textSize = 12f
            setTextColor(Color.parseColor("#DC2626"))
            visibility = View.INVISIBLE
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(errorText)

        // Keypad Grid 3x4
        val grid = GridLayout(context).apply {
            columnCount = 3
            layoutParams = LinearLayout.LayoutParams(dp(240), wrap)
        }

        val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "👆", "0", "⌫")
        digits.forEach { d ->
            val btn = Button(context).apply {
                text = d
                textSize = if (d == "⌫" || d == "👆") 18f else 22f
                setTextColor(darkBlue)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = dp(64)
                    height = dp(64)
                    setMargins(dp(8), dp(8), dp(8), dp(8))
                }
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(32).toFloat()
                }
                setOnClickListener {
                    when (d) {
                        "⌫" -> onBackspace()
                        "👆" -> onFingerprint()
                        else -> onDigit(d)
                    }
                }
            }
            grid.addView(btn)
        }
        root.addView(grid)

        // Fingerprint simulation button
        val fpBtn = Button(context).apply {
            text = "Déverrouiller par empreinte"
            setAllCaps(false)
            setTextColor(mainBlue)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(12).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(dp(240), wrap).apply {
                setMargins(0, dp(24), 0, 0)
            }
            setOnClickListener { onFingerprint() }
        }
        root.addView(fpBtn)

        setContentView(root)
    }

    private fun onDigit(digit: String) {
        if (enteredPin.length >= 4) return
        enteredPin += digit
        errorText.visibility = View.INVISIBLE
        updateDots()

        if (enteredPin.length == 4) {
            if (enteredPin == correctPin) {
                dismiss()
                onUnlock()
            } else {
                errorText.visibility = View.VISIBLE
                enteredPin = ""
                updateDots()
            }
        }
    }

    private fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorText.visibility = View.INVISIBLE
            updateDots()
        }
    }

    private fun onFingerprint() {
        Toast.makeText(context, "Empreinte reconnue ✔", Toast.LENGTH_SHORT).show()
        dismiss()
        onUnlock()
    }

    private fun updateDots() {
        dots.forEachIndexed { idx, dot ->
            val filled = idx < enteredPin.length
            dot.background = GradientDrawable().apply {
                setColor(if (filled) mainBlue else Color.TRANSPARENT)
                cornerRadius = dp(8).toFloat()
                setStroke(dp(2), mainBlue)
            }
        }
    }

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
}
