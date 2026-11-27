package com.example.acinfinity

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import org.json.*
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
class DashboardView(context: Context, private val onLogout: () -> Unit) : WebView(context) {
    private var taps = 0
    private var lastTap = 0L
    private var ready = false
    private var lastJson: String? = null

    init {
        settings.javaScriptEnabled = true
        setBackgroundColor(0xFF050608.toInt())
        addJavascriptInterface(object {
            @JavascriptInterface
            fun tapDot() = post {
                val now = System.currentTimeMillis()
                taps = if (now - lastTap < 1500) taps + 1 else 1
                lastTap = now
                if (taps >= 7) { taps = 0; onLogout() }
            }
        }, "App")
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView?, u: String?) {
                ready = true
                lastJson?.let { evaluateJavascript("render($it)", null) }
            }
        }
        loadDataWithBaseURL(null, html(), "text/html", "UTF-8", null)
    }

    fun updateData(list: List<ControllerData>) {
        val root = JSONObject()
        val f = list.firstOrNull { it.type == 20 } ?: list.getOrNull(0)
        val v = list.firstOrNull { it.type != 20 && it != f } ?: list.getOrNull(1)
        fun pack(c: ControllerData) = JSONObject()
            .put("t", String.format(Locale.US, "%.1f°C", c.tempC))
            .put("v", String.format(Locale.US, "%.2f", c.vpd))
            .put("h", String.format(Locale.US, "%.1f%%", c.rh))
            .put("ph", c.ph?.let { String.format(Locale.US, "%.1f", it) } ?: "--")
            .put("ec", c.ec?.let { String.format(Locale.US, "%.2f", it) } ?: "--")
            .put("wt", c.waterTempC?.let { String.format(Locale.US, "%.1f°", it) } ?: "--")
            .put("on", c.lightOnMin)
            .put("off", c.lightOffMin)
            .put("p", JSONArray().apply {
                c.ports.forEach {
                    put(JSONObject().put("n", it.name.uppercase(Locale.US)).put("v", it.valueText).put("on", it.isOn))
                }
            })
        if (f != null) root.put("f", pack(f))
        if (v != null) root.put("v", pack(v))
        lastJson = root.toString()
        if (ready) evaluateJavascript("render($lastJson)", null)
    }

    private fun html() = """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
*{box-sizing:border-box;margin:0;padding:0;font-family:sans-serif;user-select:none}
body{background:#050608;color:#F8FAFC;height:100vh;display:flex;overflow:hidden}
.col{width:50%;padding:14px 18px 16px;display:flex;flex-direction:column;justify-content:space-between;position:relative}
.col:first-child{border-right:1px solid #1E293B;background:radial-gradient(circle at 50% 50%,#34D39916,transparent 72%)}
.col:last-child{background:radial-gradient(circle at 50% 50%,#60A5FA16,transparent 72%)}
.hdr{display:flex;align-items:center;font-size:15px;font-weight:800;letter-spacing:.07em}
.dot{width:11px;height:11px;border-radius:50%;background:#34D399;display:inline-block;margin-right:8px}
.hyd{position:absolute;top:46px;left:22px;display:flex;gap:14px}
.mring{width:50px;height:50px;border-radius:50%;border:1.8px solid #34D399;box-shadow:0 0 10px -2px #34D399;display:flex;flex-direction:column;align-items:center;justify-content:center}
.mval{font-size:12px;font-weight:800;color:#F8FAFC}
.mlbl{font-size:8px;color:#94A3B8;margin-top:1px;letter-spacing:.06em}
.dial{position:absolute;top:10px;right:14px;width:105px;height:105px}
.rings{display:flex;justify-content:space-between;align-items:center;padding:0 2px;margin-top:52px}
.ring{width:28vmin;height:28vmin;max-width:166px;max-height:166px;border-radius:50%;border:2.5px solid var(--rc,var(--c));box-shadow:0 0 14px -2px var(--rc,var(--c));display:flex;flex-direction:column;align-items:center;justify-content:center;transition:border-color .6s,box-shadow .6s}
.ring.vpd{width:34vmin;height:34vmin;max-width:198px;max-height:198px;border-width:3.5px;animation:br var(--bs,4s) ease-in-out infinite}
.ripple{animation:rp .9s ease-out!important}
@keyframes br{0%,100%{box-shadow:0 0 12px -2px var(--rc,var(--c))}50%{box-shadow:0 0 28px 3px var(--rc,var(--c))}}
@keyframes rp{0%{box-shadow:0 0 0 0 var(--rc,var(--c))}100%{box-shadow:0 0 0 26px transparent}}
.val{font-size:28px;font-weight:800}
.vpd .val{font-size:39px}
.lbl{font-size:11px;color:#94A3B8;margin-top:4px;letter-spacing:.08em}
.ports{display:flex;justify-content:space-around;padding-bottom:2px}
.pt{display:flex;flex-direction:column;align-items:center;font-size:12px;font-weight:700;color:#94A3B8}
.pval{font-size:16px;font-weight:800;color:#F8FAFC;margin-bottom:5px}
.pcirc{width:54px;height:54px;border-radius:50%;border:2.5px solid #334155;display:flex;align-items:center;justify-content:center;color:#64748B}
.pcirc svg{width:24px;height:24px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
.on .pcirc{border-color:var(--c);color:var(--c);box-shadow:0 0 14px var(--c)}
.spin{animation:sp var(--spd,2s) linear infinite}
@keyframes sp{to{transform:rotate(360deg)}}
</style>
</head>
