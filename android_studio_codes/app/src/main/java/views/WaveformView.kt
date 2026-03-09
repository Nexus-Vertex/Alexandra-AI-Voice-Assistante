package com.example.myapp.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
    }

    private var isAnimating = false
    private var time = 0f
    private val BAR_COUNT = 32

    private val speeds = FloatArray(BAR_COUNT) { i ->
        0.018f + (i % 7) * 0.004f
    }
    private val waveSpeed = 0.012f
    private val phaseOffsets = FloatArray(BAR_COUNT) { i ->
        i * (Math.PI.toFloat() * 2f / BAR_COUNT)
    }

    // ✅ Hauteurs fixes quand immobile
    private val idleHeights = FloatArray(BAR_COUNT) { i ->
        val center = 1f - abs(i - BAR_COUNT / 2f) / (BAR_COUNT / 2f)
        0.08f + center * 0.18f
    }

    fun startAnimating() {
        if (isAnimating) return
        isAnimating = true
        animateLoop()
    }

    fun stopAnimating() {
        isAnimating = false
        invalidate() // ✅ Reste visible mais immobile
    }

    fun updateAmplitude(amplitude: Float) {}

    private fun animateLoop() {
        if (!isAnimating) return
        time += 1f
        invalidate()
        postDelayed({ animateLoop() }, 40)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2f
        val totalSpacing = w * 0.05f
        val barWidth = (w - totalSpacing) / (BAR_COUNT * 1.6f)
        val step = (w - totalSpacing) / BAR_COUNT

        for (i in 0 until BAR_COUNT) {
            val x = totalSpacing / 2f + i * step + step / 2f

            val amp = if (isAnimating) {
                // ✅ Barres qui bougent quand son détecté
                val amp1 = sin(time * speeds[i] + phaseOffsets[i])
                val amp2 = sin(time * waveSpeed + i * 0.25f)
                val amp3 = sin(time * speeds[i] * 0.5f + phaseOffsets[i] * 0.7f)
                abs(amp1 * 0.5f + amp2 * 0.35f + amp3 * 0.15f).coerceIn(0.08f, 1f)
            } else {
                // ✅ Petites barres fixes immobiles
                idleHeights[i]
            }

            val barH = (0.10f + amp * 0.90f) * midY * 0.88f
            val top    = midY - barH
            val bottom = midY + barH

            val centerRatio = 1f - abs(i - BAR_COUNT / 2f) / (BAR_COUNT / 2f)
            // Moins lumineux en mode immobile
            val intensite = if (isAnimating) amp else amp * 0.4f

            val color1 = interpolateColor(0xFF3A0080.toInt(), 0xFFCC88FF.toInt(), centerRatio * intensite)
            val color2 = interpolateColor(0xFF6A00CC.toInt(), 0xFFFFFFFF.toInt(), centerRatio * intensite * 0.7f)

            glowPaint.shader = LinearGradient(x, top, x, bottom,
                intArrayOf(Color.TRANSPARENT, adjustAlpha(color1, 0.5f),
                    adjustAlpha(color2, 0.8f), adjustAlpha(color1, 0.5f), Color.TRANSPARENT),
                floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(x - barWidth * 1.5f, top, x + barWidth * 1.5f, bottom),
                barWidth, barWidth, glowPaint)

            paint.shader = LinearGradient(x, top, x, bottom,
                intArrayOf(Color.TRANSPARENT, color1, color2, color1, Color.TRANSPARENT),
                floatArrayOf(0f, 0.15f, 0.5f, 0.85f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(x - barWidth / 2f, top, x + barWidth / 2f, bottom),
                barWidth / 2f, barWidth / 2f, paint)
        }
    }

    private fun interpolateColor(c1: Int, c2: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        return Color.argb(
            Color.alpha(c1) + ((Color.alpha(c2) - Color.alpha(c1)) * r).toInt(),
            Color.red(c1)   + ((Color.red(c2)   - Color.red(c1))   * r).toInt(),
            Color.green(c1) + ((Color.green(c2) - Color.green(c1)) * r).toInt(),
            Color.blue(c1)  + ((Color.blue(c2)  - Color.blue(c1))  * r).toInt()
        )
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        return Color.argb((Color.alpha(color) * factor).toInt().coerceIn(0, 255),
            Color.red(color), Color.green(color), Color.blue(color))
    }
}