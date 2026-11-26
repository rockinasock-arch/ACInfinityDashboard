package com.example.acinfinity

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import org.json.*
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
class DashboardView(context: Context, private val onLogout: () -> Unit) : WebView(context) {
    private var taps = 0; private var lastTap = 0L; private var ready = false; private var lastJson: String? = null

    init {
        settings.javaScriptEnabled = true; setBackgroundColor(0xFF050608.toInt())
        addJavascriptInterface(object {
            @JavascriptInterface fun tapDot() = post {
                val now = System.currentTimeMillis(); taps = if (now - lastTap < 1500) taps + 1 else 1; lastTap = now
                if (taps >= 7) { taps = 0; onLogout() }
            }
        }, "App")
        webViewClient = object : WebViewClient() { override fun onPageFinished(v: WebView?, u: String?) { ready = true; lastJson?.let { evaluateJavascript("render($it)", null) } } }
        loadDataWithBaseURL(null, html(), "text/html", "UTF-8", null)
    }

    fun updateData(list: List<ControllerData>) {
        val root = JSONObject(); val f = list.firstOrNull { it.type == 20 } ?: list.getOrNull(0); val v = list.firstOrNull { it.type != 20 && it != f } ?: list.getOrNull(1)
        fun pack(c: ControllerData) = JSONObject().put("t", String.format(Locale.US, "%.1f°C", c.tempC)).put("v", String.format(Locale.US, "%.2f", c.vpd)).put("h", String.format(Locale.US, "%.1f%%", c.rh))
            .put("ph", c.ph?.let { String.format(Locale.US, "%.1f", it) } ?: "--").put("ec", c.ec?.let { String.format(Locale.US, "%.2f", it) } ?: "--").put("wt", c.waterTempC?.let { String.format(Locale.US, "%.1f°C", it) } ?: "--")
            .put("dbg", c.debugText)
            .put("p", JSONArray().apply { c.ports.forEach { put(JSONObject().put("n", it.name.uppercase(Locale.US)).put("v", it.valueText).put("on", it.isOn)) } })
        if (f != null) root.put("f", pack(f)); if (v != null) root.put("v", pack(v))
        lastJson = root.toString(); if (ready) evaluateJavascript("render($lastJson)", null)
    }

    private fun html() = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
*{box-sizing:border-box;margin:0;padding:0;font-family:sans-serif;user-select:none}body{background:#050608;color:#F8FAFC;height:100vh;display:flex;overflow:hidden}
.col{width:50%;padding:14px 18px;display:flex;flex-direction:column;justify-content:space-between;position:relative}
.col:first-child{border-right:1px solid #1E293B;background:radial-gradient(circle at 50% 45%,#34D39916,transparent 72%)}.col:last-child{background:radial-gradient(circle at 50% 45%,#60A5FA16,transparent 72%)}
.hdr{display:flex;align-items:center;font-size:14px;font-weight:800;letter-spacing:.07em}.dot{width:11px;height:11px;border-radius:50%;background:#34D399;display:inline-block;margin-right:8px}
.dbg{position:absolute;top:10px;right:14px;width:58%;font-size:9px;color:#FACC15;word-break:break-all;line-height:1.15;text-align:right}
.rings{display:flex;justify-content:space-between;align-items:center;padding:0 4px}
.ring{width:29vmin;height:29vmin;max-width:172px;max-height:172px;border-radius:50%;border:2.5px solid var(--rc,var(--c));box-shadow:0 0 14px -2px var(--rc,var(--c));display:flex;flex-direction:column;align-items:center;justify-content:center}
.ring.vpd{width:35vmin;height:35vmin;max-width:205px;max-height:205px;border-width:3.5px}
.val{font-size:29px;font-weight:800}.vpd .val{font-size:40px}.lbl{font-size:11px;color:#94A3B8;margin-top:4px;letter-spacing:.08em}
.hydro{display:flex;justify-content:center;gap:14px;padding:8px 0;border-top:1px solid #1E293B;border-bottom:1px solid #1E293B}
.pill{background:#0F172A;border:1px solid #1E293B;border-radius:99px;padding:4px 14px;font-size:13px;font-weight:700}.ports{display:flex;justify-content:space-around}
.pt{display:flex;flex-direction:column;align-items:center;font-size:11px;color:#94A3B8}.pval{font-size:14px;font-weight:800;color:#F8FAFC;margin-bottom:4px}
.pcirc{width:46px;height:46px;border-radius:50%;border:2px solid #334155;display:flex;align-items:center;justify-content:center;color:#64748B}
.pcirc svg{width:22px;height:22px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}.on .pcirc{border-color:var(--c);color:var(--c);box-shadow:0 0 12px var(--c)}
</style></head><body>
<div class="col" style="--c:#34D399"><div class="hdr"><span class="dot" onclick="App.tapDot()"></span><span style="color:#34D399">FLOWER AI+</span></div><div class="dbg" id="fd"></div>
<div class="rings"><div class="ring"><div class="val" id="ft">--°C</div><div class="lbl">TEMP</div></div><div class="ring vpd"><div class="val" id="fv">--</div><div class="lbl">VPD kPa</div></div><div class="ring"><div class="val" id="fh">--%</div><div class="lbl">RH</div></div></div>
<div class="hydro"><span class="pill" id="ph">pH --</span><span class="pill" id="ec">EC --</span><span class="pill" id="wt">WATER --°C</span></div><div class="ports" id="fp"></div></div>
<div class="col" style="--c:#60A5FA"><div class="hdr"><span style="color:#60A5FA">VEG 69+</span></div><div class="dbg" id="vd"></div>
<div class="rings"><div class="ring"><div class="val" id="vt">--°C</div><div class="lbl">TEMP</div></div><div class="ring vpd"><div class="val" id="vv">--</div><div class="lbl">VPD kPa</div></div><div class="ring"><div class="val" id="vh">--%</div><div class="lbl">RH</div></div></div>
<div class="hydro" style="visibility:hidden"><span class="pill">--</span></div><div class="ports" id="vp"></div></div>
<script>
const S={fan:'<circle cx="12" cy="12" r="2"/><path d="M12 10c0-3 2-6 4.5-5S18 9 14 11m-1 2.5c2.5 1.5 4 4.5 2 6s-4.5-.5-4-4.5m-2 0c-2.5 1.5-6 1.5-6-1s3-3.5 6-1"/>',bulb:'<path d="M9 18h6m-5 3h4M9 14c-2-1.5-3-3.5-3-6a6 6 0 1 1 12 0c0 2.5-1 4.5-3 6v1H9v-1z"/>',heat:'<path d="M12 2c2 4 6 6.5 6 11a6 6 0 1 1-12 0c0-2.5 1.5-4.5 3-6-.5 2 1 3 2 3 0-3 1-5.5 1-8z"/>',dehum:'<path d="M12 3l5.5 6.5a7.5 7.5 0 1 1-11 0L12 3z"/><path d="M12 10v6m-2.5-2.5L12 16l2.5-2.5"/>',hum:'<path d="M12 3l5.5 6.5a7.5 7.5 0 1 1-11 0L12 3z"/><path d="M9.5 16c1-1.5 4-1.5 5 0M10.5 13.5c.8-1 2.2-1 3 0"/>'};
function svgFor(n){return n.includes('DEHUM')?S.dehum:n.includes('HUMID')?S.hum:n.includes('FAN')?S.fan:n.includes('HEAT')?S.heat:n.includes('LIGHT')?S.bulb:'<circle cx="12" cy="12" r="5"/>';}
function ports(id,list){document.getElementById(id).innerHTML=list.map(p=>'<div class="pt '+(p.on?'on':'')+'"><div class="pval">'+p.v+'</div><div class="pcirc"><svg viewBox="0 0 24 24">'+svgFor(p.n)+'</svg></div><div style="margin-top:4px">'+p.n+'</div></div>').join('');}
function render(d){['f','v'].forEach(k=>{if(!d[k])return;document.getElementById(k+'t').textContent=d[k].t;document.getElementById(k+'v').textContent=d[k].v;document.getElementById(k+'h').textContent=d[k].h;document.getElementById(k+'d').textContent=d[k].dbg||'';ports(k+'p',d[k].p);});if(d.f){document.getElementById('ph').textContent='pH '+d.f.ph;document.getElementById('ec').textContent='EC '+d.f.ec;document.getElementById('wt').textContent='WATER '+d.f.wt;}}
</script></body></html>"""
}
