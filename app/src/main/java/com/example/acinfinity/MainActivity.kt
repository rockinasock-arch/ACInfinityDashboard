package com.example.acinfinity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class DashboardView(context: Context, private val onLogout: () -> Unit) : View(context) {
    private var controllers: List<ControllerData> = emptyList()
    private var animPhase = 0f
    private var dotTaps = 0
    private var lastTapMs = 0L

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0A0B0E") }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#12151C") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#EC4899") }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A1F2B") }
    private val ringBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 16f; color = Color.parseColor("#252A36") }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 16f; color = Color.parseColor("#F43F85") }
    private val portRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 3.5f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#94A3B8") }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 6000L; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
        addUpdateListener { animPhase = it.animatedValue as Float; invalidate() }
    }

    init { animator.start() }

    fun updateData(list: List<ControllerData>) { controllers = list; invalidate() }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && event.x < width * 0.15f && event.y < height * 0.20f) {
            val now = System.currentTimeMillis()
            dotTaps = if (now - lastTapMs < 1500L) dotTaps + 1 else 1
            lastTapMs = now
            if (dotTaps >= 7) { dotTaps = 0; onLogout() }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        if (controllers.isEmpty()) {
            textPaint.textSize = h * 0.05f; textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Syncing AC Infinity Cloud...", w / 2f, h / 2f, textPaint)
            return
        }
        val pad = w * 0.02f; val gap = w * 0.02f
        val count = min(2, controllers.size)
        val cardW = (w - pad * 2 - gap * (count - 1)) / count
        val cardH = h - pad * 2
        for (i in 0 until count) drawCard(canvas, controllers[i], pad + i * (cardW + gap), pad, cardW, cardH)
    }

    private fun drawCard(canvas: Canvas, c: ControllerData, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(rect, 32f, 32f, borderPaint)

        val pulse = 0.65f + 0.35f * sin(Math.toRadians(animPhase.toDouble() * 2)).toFloat()
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981"); alpha = (pulse * 255).toInt() }
        canvas.drawCircle(x + w * 0.06f, y + h * 0.09f, h * 0.016f, dotPaint)

        textPaint.textAlign = Paint.Align.LEFT; textPaint.textSize = h * 0.055f
        canvas.drawText(c.name, x + w * 0.10f, y + h * 0.108f, textPaint)

        val badgeR = h * 0.030f
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#065F46") }
        canvas.drawCircle(x + w * 0.85f, y + h * 0.09f, badgeR, badgePaint)
        canvas.drawCircle(x + w * 0.92f, y + h * 0.09f, badgeR, badgePaint)

        val cx = x + w * 0.50f; val cy = y + h * 0.38f
        val radius = min(w * 0.17f, h * 0.19f)
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(oval, 0f, 360f, false, ringBgPaint)
        canvas.drawArc(oval, -90f, ((c.vpd / 2.5).coerceIn(0.05, 1.0) * 360.0).toFloat(), false, ringPaint)

        textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = radius * 0.56f
        canvas.drawText(String.format("%.2f", c.vpd), cx, cy + radius * 0.14f, textPaint)
        subPaint.textAlign = Paint.Align.CENTER; subPaint.textSize = radius * 0.25f
        canvas.drawText("VPD kPa", cx, cy + radius * 0.48f, subPaint)

        val lx = x + w * 0.18f
        textPaint.textSize = h * 0.072f
        val tArrow = if (c.tempTrend > 0) "↑" else if (c.tempTrend < 0) "↓" else ""
        canvas.drawText(String.format("%.1f°%s", c.tempC, tArrow), lx, cy + h * 0.02f, textPaint)
        subPaint.textSize = h * 0.032f
        canvas.drawText("TEMP °C", lx, cy + h * 0.075f, subPaint)

        val rx = x + w * 0.82f
        textPaint.textSize = h * 0.072f
        val hArrow = if (c.rhTrend > 0) "↑" else if (c.rhTrend < 0) "↓" else ""
        canvas.drawText(String.format("%.1f%%%s", c.rh, hArrow), rx, cy + h * 0.02f, textPaint)
        subPaint.textSize = h * 0.032f
        canvas.drawText("HUMIDITY", rx, cy + h * 0.075f, subPaint)

        if (c.ph != null || c.ec != null || c.waterTempC != null) {
            val pillRect = RectF(x + w * 0.08f, y + h * 0.61f, x + w * 0.92f, y + h * 0.70f)
            canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)
            subPaint.textAlign = Paint.Align.CENTER; subPaint.textSize = h * 0.034f
            val phStr = c.ph?.let { String.format("pH %.1f", it) } ?: "pH --"
            val ecStr = c.ec?.let { String.format("EC %.2f", it) } ?: "EC --"
            val wtStr = c.waterTempC?.let { String.format("H2O %.1f°", it) } ?: "H2O --"
            canvas.drawText("$phStr    •    $ecStr    •    $wtStr", cx, y + h * 0.668f, subPaint)
        }

        val pCount = c.ports.size.coerceAtLeast(1)
        val step = (w * 0.88f) / pCount
        val pRadius = min(step * 0.36f, h * 0.082f)
        val py = y + h * 0.84f

        for (idx in c.ports.indices) {
            val port = c.ports[idx]
            val px = x + w * 0.06f + step * (idx + 0.5f)
            val activeColor = if (port.isOn) Color.parseColor("#14B8A6") else Color.parseColor("#475569")
            portRingPaint.color = activeColor
            iconPaint.color = activeColor
            canvas.drawCircle(px, py, pRadius, pillPaint)
            canvas.drawCircle(px, py, pRadius, portRingPaint)

            drawDeviceIcon(canvas, port, px, py - pRadius * 0.34f, pRadius * 0.28f)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = if (port.valueText.length > 1) pRadius * 0.50f else pRadius * 0.66f
            canvas.drawText(port.valueText, px, py + pRadius * 0.52f, textPaint)
        }
    }

    private fun drawDeviceIcon(canvas: Canvas, p: PortData, cx: Float, cy: Float, r: Float) {
        val n = p.name.lowercase()
        when {
            n.contains("fan") -> {
                val rot = if (p.isOn) animPhase * 2f else 0f
                for (b in 0..2) {
                    val rad = Math.toRadians((rot + b * 120).toDouble())
                    canvas.drawLine(cx, cy, cx + cos(rad).toFloat() * r, cy + sin(rad).toFloat() * r, iconPaint)
                }
            }
            n.contains("light") -> {
                canvas.drawCircle(cx, cy, r * 0.45f, iconPaint)
                for (b in 0..5) {
                    val rad = Math.toRadians((b * 60).toDouble())
                    canvas.drawLine(cx + cos(rad).toFloat() * r * 0.65f, cy + sin(rad).toFloat() * r * 0.65f, cx + cos(rad).toFloat() * r, cy + sin(rad).toFloat() * r, iconPaint)
                }
            }
            n.contains("heat") -> {
                val path = Path().apply { moveTo(cx, cy - r); quadTo(cx + r, cy, cx, cy + r); quadTo(cx - r, cy, cx, cy - r) }
                canvas.drawPath(path, iconPaint)
            }
            else -> {
                val path = Path().apply { moveTo(cx, cy - r); lineTo(cx + r * 0.75f, cy + r * 0.35f); arcTo(RectF(cx - r * 0.75f, cy - r * 0.3f, cx + r * 0.75f, cy + r), 0f, 180f); close() }
                canvas.drawPath(path, iconPaint)
            }
        }
    }
}
