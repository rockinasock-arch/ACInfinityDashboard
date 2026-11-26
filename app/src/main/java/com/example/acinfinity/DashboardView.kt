package com.example.acinfinity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.*
import android.view.animation.LinearInterpolator
import kotlin.math.*

class DashboardView(context: Context, private val onLogout: () -> Unit) : View(context) {
    private var controllers: List<ControllerData> = emptyList()
    private var animPhase = 0f; private var dotTaps = 0; private var lastTapMs = 0L
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#08090D") }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10131A") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = Color.parseColor("#831843") }
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#161B26") }
    private val ringBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 20f; color = Color.parseColor("#1E2430") }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 20f; color = Color.parseColor("#F43F85") }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val portRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4.5f }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 4f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create("sans-serif-light", Typeface.NORMAL) }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#64748B"); typeface = Typeface.create("sans-serif-medium", Typeface.BOLD) }
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply { duration = 6000L; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); addUpdateListener { animPhase = it.animatedValue as Float; invalidate() } }

    init { animator.start() }
    fun updateData(list: List<ControllerData>) { controllers = list; invalidate() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN && e.x < width * 0.15f && e.y < height * 0.20f) {
            val now = System.currentTimeMillis(); dotTaps = if (now - lastTapMs < 1500L) dotTaps + 1 else 1; lastTapMs = now
            if (dotTaps >= 7) { dotTaps = 0; onLogout() }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        if (controllers.isEmpty()) { textPaint.textSize = h * 0.045f; textPaint.textAlign = Paint.Align.CENTER; canvas.drawText("Syncing AC Infinity Cloud...", w / 2f, h / 2f, textPaint); return }
        val pad = w * 0.018f; val count = min(2, controllers.size); val cardW = (w - pad * (count + 1)) / count
        for (i in 0 until count) drawCard(canvas, controllers[i], pad + i * (cardW + pad), pad, cardW, h - pad * 2)
    }

    private fun drawCard(canvas: Canvas, c: ControllerData, x: Float, y: Float, w: Float, h: Float) {
        val rect = RectF(x, y, x + w, y + h); canvas.drawRoundRect(rect, 36f, 36f, cardPaint); canvas.drawRoundRect(rect, 36f, 36f, borderPaint)
        val pulse = 0.6f + 0.4f * sin(Math.toRadians(animPhase.toDouble() * 2)).toFloat()
        canvas.drawCircle(x + w * 0.06f, y + h * 0.085f, h * 0.014f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#10B981"); alpha = (pulse * 255).toInt() })
        textPaint.textAlign = Paint.Align.LEFT; textPaint.textSize = h * 0.048f; canvas.drawText(c.name.uppercase(), x + w * 0.095f, y + h * 0.102f, textPaint)
        val bR = h * 0.028f; val bBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#064E3B") }; val bRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = Color.parseColor("#10B981") }; val bTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399"); textSize = bR * 0.85f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
        canvas.drawCircle(x + w * 0.84f, y + h * 0.085f, bR, bBg); canvas.drawCircle(x + w * 0.84f, y + h * 0.085f, bR, bRing); canvas.drawText(if (c.type == 20) "AI" else "69", x + w * 0.84f, y + h * 0.085f + bR * 0.32f, bTxt)
        canvas.drawCircle(x + w * 0.92f, y + h * 0.085f, bR, bBg); canvas.drawCircle(x + w * 0.92f, y + h * 0.085f, bR, bRing); canvas.drawText("✓", x + w * 0.92f, y + h * 0.085f + bR * 0.34f, bTxt)
        val cx = x + w * 0.50f; val cy = y + h * 0.38f; val r = min(w * 0.165f, h * 0.185f)
        glowPaint.shader = RadialGradient(cx, cy, r * 1.35f, Color.parseColor("#33F43F85"), Color.TRANSPARENT, Shader.TileMode.CLAMP); canvas.drawCircle(cx, cy, r * 1.35f, glowPaint)
        val oval = RectF(cx - r, cy - r, cx + r, cy + r); canvas.drawArc(oval, 0f, 360f, false, ringBgPaint); canvas.drawArc(oval, -90f, ((c.vpd / 2.2).coerceIn(0.05, 1.0) * 360.0).toFloat(), false, ringPaint)
        numPaint.textAlign = Paint.Align.CENTER; numPaint.textSize = r * 0.62f; canvas.drawText(String.format("%.2f", c.vpd), cx, cy + r * 0.16f, numPaint)
        subPaint.textAlign = Paint.Align.CENTER; subPaint.textSize = r * 0.22f; canvas.drawText("VPD kPa", cx, cy + r * 0.50f, subPaint)
        numPaint.textSize = h * 0.076f; subPaint.textSize = h * 0.028f
        canvas.drawText(String.format("%.1f°%s", c.tempC, if (c.tempTrend > 0) "↑" else if (c.tempTrend < 0) "↓" else ""), x + w * 0.175f, cy + h * 0.02f, numPaint); canvas.drawText("TEMP °C", x + w * 0.175f, cy + h * 0.072f, subPaint)
        canvas.drawText(String.format("%.1f%%%s", c.rh, if (c.rhTrend > 0) "↑" else if (c.rhTrend < 0) "↓" else ""), x + w * 0.825f, cy + h * 0.02f, numPaint); canvas.drawText("HUMIDITY", x + w * 0.825f, cy + h * 0.072f, subPaint)
        if (c.ph != null || c.ec != null || c.waterTempC != null) {
            canvas.drawRoundRect(RectF(x + w * 0.08f, y + h * 0.605f, x + w * 0.92f, y + h * 0.695f), 26f, 26f, pillPaint)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = h * 0.032f
            canvas.drawText("${c.ph?.let { String.format("pH %.1f", it) } ?: "pH --"}   •   ${c.ec?.let { String.format("EC %.2f", it) } ?: "EC --"}   •   ${c.waterTempC?.let { String.format("H2O %.1f°", it) } ?: "H2O --"}", cx, y + h * 0.662f, textPaint)
        }
        val step = (w * 0.88f) / c.ports.size.coerceAtLeast(1); val pR = min(step * 0.36f, h * 0.084f); val py = y + h * 0.84f
        for (idx in c.ports.indices) {
            val port = c.ports[idx]; val px = x + w * 0.06f + step * (idx + 0.5f)
            portRingPaint.color = if (port.isOn) Color.parseColor("#14B8A6") else Color.parseColor("#334155"); iconPaint.color = if (port.isOn) Color.parseColor("#2DD4BF") else Color.parseColor("#64748B")
            canvas.drawCircle(px, py, pR, pillPaint); canvas.drawCircle(px, py, pR, portRingPaint); drawDeviceIcon(canvas, port, px, py - pR * 0.32f, pR * 0.26f)
            textPaint.textAlign = Paint.Align.CENTER; textPaint.textSize = if (port.valueText.length > 1) pR * 0.46f else pR * 0.64f; canvas.drawText(port.valueText, px, py + pR * 0.54f, textPaint)
        }
    }

    private fun drawDeviceIcon(canvas: Canvas, p: PortData, cx: Float, cy: Float, r: Float) {
        val n = p.name.lowercase()
        when {
            n.contains("fan") -> { val rot = if (p.isOn) animPhase * 2f else 0f; for (b in 0..2) { val rad = Math.toRadians((rot + b * 120).toDouble()); canvas.drawLine(cx, cy, cx + cos(rad).toFloat() * r, cy + sin(rad).toFloat() * r, iconPaint) } }
            n.contains("light") -> { canvas.drawCircle(cx, cy, r * 0.44f, iconPaint); for (b in 0..5) { val rad = Math.toRadians((b * 60).toDouble()); canvas.drawLine(cx + cos(rad).toFloat() * r * 0.66f, cy + sin(rad).toFloat() * r * 0.66f, cx + cos(rad).toFloat() * r, cy + sin(rad).toFloat() * r, iconPaint) } }
            n.contains("heat") -> canvas.drawPath(Path().apply { moveTo(cx, cy - r); quadTo(cx + r, cy, cx, cy + r); quadTo(cx - r, cy, cx, cy - r) }, iconPaint)
            else -> canvas.drawPath(Path().apply { moveTo(cx, cy - r); lineTo(cx + r * 0.72f, cy + r * 0.35f); arcTo(RectF(cx - r * 0.72f, cy - r * 0.3f, cx + r * 0.72f, cy + r), 0f, 180f); close() }, iconPaint)
        }
    }
}
