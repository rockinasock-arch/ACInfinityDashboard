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
    private var lastUpdateMs = System.currentTimeMillis()
    private var tapStreak = 0
    private var easterToast = ""
    private var easterUntil = 0L

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0A0B0E") }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#12151C") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#EC4899")
    }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A1F2B") }
    private val ringBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        color = Color.parseColor("#252A36")
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 16f
        color = Color.parseColor("#F43F85")
    }
    private val portRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#14B8A6")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#94A3B8") }

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 6000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animPhase = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        animator.start()
        setOnLongClickListener {
            onLogout()
            true
        }
    }

    fun updateData(list: List<ControllerData>) {
        controllers = list
        lastUpdateMs = System.currentTimeMillis()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            tapStreak++
            if (tapStreak >= 5) {
                tapStreak = 0
                easterToast = listOf(
                    "Plants vibing at optimal VPD",
                    "Fans spinning like tiny jet engines",
                    "Photosynthesis mode: ENGAGED",
                    "Leaves are praying to the LED gods",
                    "Zero drama in the grow tent"
                ).random()
                easterUntil = System.currentTimeMillis() + 3500L
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (controllers.isEmpty()) {
            textPaint.textSize = h * 0.05f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Syncing AC Infinity Cloud...", w / 2f, h / 2f, textPaint)
            return
        }

        val pad = w * 0.02f
        val gap = w * 0.02f
        val count = min(2, controllers.size)
        val cardW = (w - pad * 2 - gap * (count - 1)) / count
        val cardH = h - pad * 2

        for (i in 0 until count) {
            val left = pad + i * (cardW + gap)
            drawCard(canvas, controllers[i], left, pad, cardW, cardH)
        }

        if (System.currentTimeMillis() < easterUntil) {
            subPaint.textSize = h * 0.032f
            subPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(easterToast, w / 2f, h - pad * 0.35f, subPaint)
        }
    }

    private fun drawCard(canvas: Canvas, c: ControllerData, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(rect, 32f, 32f, borderPaint)

        // Header
        val pulse = 0.65f + 0.35f * sin(Math.toRadians(animPhase.toDouble() * 2)).toFloat()
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            alpha = (pulse * 255).toInt()
        }
        canvas.drawCircle(x + w * 0.06f, y + h * 0.09f, h * 0.016f, dotPaint)

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = h * 0.055f
        canvas.drawText(c.name, x + w * 0.10f, y + h * 0.108f, textPaint)

        // Stage badges top-right
        val badgeR = h * 0.032f
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#065F46") }
        canvas.drawCircle(x + w * 0.84f, y + h * 0.09f, badgeR, badgePaint)
        canvas.drawCircle(x + w * 0.92f, y + h * 0.09f, badgeR, badgePaint)

        // Center VPD Ring
        val cx = x + w * 0.50f
        val cy = y + h * 0.38f
        val radius = min(w * 0.17f, h * 0.19f)
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(oval, 0f, 360f, false, ringBgPaint)
        val sweep = ((c.vpd / 2.5).coerceIn(0.05, 1.0) * 360.0).toFloat()
        canvas.drawArc(oval, -90f, sweep, false, ringPaint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = radius * 0.56f
        canvas.drawText(String.format("%.2f", c.vpd), cx, cy + radius * 0.14f, textPaint)
        subPaint.textAlign = Paint.Align.CENTER
        subPaint.textSize = radius * 0.26f
        canvas.drawText("VPD kPa", cx, cy + radius * 0.48f, subPaint)

        // Left: Temperature C
        val lx = x + w * 0.18f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = h * 0.075f
        val tArrow = if (c.tempTrend > 0) "↑" else if (c.tempTrend < 0) "↓" else ""
        canvas.drawText(String.format("%.1f°%s", c.tempC, tArrow), lx, cy + h * 0.02f, textPaint)
        subPaint.textSize = h * 0.033f
        canvas.drawText("TEMP °C", lx, cy + h * 0.075f, subPaint)

        // Right: Humidity %
        val rx = x + w * 0.82f
        textPaint.textSize = h * 0.075f
        val hArrow = if (c.rhTrend > 0) "↑" else if (c.rhTrend < 0) "↓" else ""
        canvas.drawText(String.format("%.0f%%%s", c.rh, hArrow), rx, cy + h * 0.02f, textPaint)
        subPaint.textSize = h * 0.033f
        canvas.drawText("HUMIDITY", rx, cy + h * 0.075f, subPaint)

        // Hydro / Sensor Pill Bar (if sensors present)
        if (c.ph != null || c.ec != null || c.waterTempC != null) {
            val pillRect = RectF(x + w * 0.08f, y + h * 0.61f, x + w * 0.92f, y + h * 0.70f)
            canvas.drawRoundRect(pillRect, 24f, 24f, pillPaint)
            subPaint.textAlign = Paint.Align.CENTER
            subPaint.textSize = h * 0.034f
            val phStr = c.ph?.let { String.format("pH %.1f", it) } ?: "pH --"
            val ecStr = c.ec?.let { String.format("EC %.1f", it) } ?: "EC --"
            val wtStr = c.waterTempC?.let { String.format("H2O %.1f°", it) } ?: "H2O --"
            canvas.drawText("$phStr    •    $ecStr    •    $wtStr", cx, y + h * 0.668f, subPaint)
        }

        // Bottom Port Badges
        val activePorts = c.ports.filter { it.online }.ifEmpty { c.ports.take(4) }
        val pCount = activePorts.size.coerceAtLeast(1)
        val pAreaW = w * 0.88f
        val step = pAreaW / pCount
        val pRadius = min(step * 0.36f, h * 0.075f)
        val py = y + h * 0.84f

        for (idx in activePorts.indices) {
            val port = activePorts[idx]
            val px = x + w * 0.06f + step * (idx + 0.5f)
            portRingPaint.color = if (port.speed > 0) Color.parseColor("#14B8A6") else Color.parseColor("#334155")
            canvas.drawCircle(px, py, pRadius, pillPaint)
            canvas.drawCircle(px, py, pRadius, portRingPaint)

            // Spinning fan blades or glow dot
            if (port.speed > 0) {
                val angle = animPhase * (0.5f + port.speed * 0.25f)
                val bladeLen = pRadius * 0.35f
                val bx = px + cos(Math.toRadians(angle.toDouble())).toFloat() * bladeLen
                val by = py - pRadius * 0.35f + sin(Math.toRadians(angle.toDouble())).toFloat() * bladeLen
                canvas.drawLine(px, py - pRadius * 0.35f, bx, by, portRingPaint)
            }

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = pRadius * 0.68f
            canvas.drawText("${port.speed}", px, py + pRadius * 0.42f, textPaint)
        }
    }
}
