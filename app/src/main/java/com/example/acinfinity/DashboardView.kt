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
            .put("on", c.lightOnMin).put("off", c.lightOffMin)
            .put("p", JSONArray().apply { c.ports.forEach { put(JSONObject().put("n", it.name.uppercase(Locale.US)).put("v", it.valueText).put("on", it.isOn)) } })
        if (f != null) root.put("f", pack(f)); if (v != null) root.put("v", pack(v))
        lastJson = root.toString(); if (ready) evaluateJavascript("render($lastJson)", null)
    }

    private fun html() = """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
*{box-sizing:border-box;margin:0;padding:0;font-family:sans-serif;user-select:none}body{background:#050608;color:#F8FAFC;height:100vh;display:flex;overflow:hidden}
.col{width:50%;padding:14px 18px;display:flex;flex-direction:column;justify-content:space-between;position:relative}
.col:first-child{border-right:1px solid #1E293B;background:radial-gradient(circle at 50% 45%,#34D39916,transparent 72%)}.col:last-child{background:radial-gradient(circle at 50% 45%,#60A5FA16,transparent 72%)}
.hdr{display:flex;align-items:center;font-size:14px;font-weight:800;letter-spacing:.07em}.dot{width:11px;height:11px;border-radius:50%;background:#34D399;display:inline-block;margin-right:8px}
.dial{position:absolute;top:12px;right:18px;width:62px;height:62px}.rings{display:flex;justify-content:space-between;align-items:center;padding:0 4px}
.ring{width:29vmin;height:29vmin;max-width:172px;max-height:172px;border-radius:50%;border:2.5px solid var(--rc,var(--c));box-shadow:0 0 14px -2px var(--rc,var(--c));display:flex;flex-direction:column;align-items:center;justify-content:center;transition:border-color .6s,box-shadow .6s}
.ring.vpd{width:35vmin;height:35vmin;max-width:205px;max-height:205px;border-width:3.5px;animation:br var(--bs,4s) ease-in-out infinite}
.ripple{animation:rp .9s ease-out!important}@keyframes br{0%,100%{box-shadow:0 0 12px -2px var(--rc,var(--c))}50%{box-shadow:0 0 28px 3px var(--rc,var(--c))}}@keyframes rp{0%{box-shadow:0 0 0 0 var(--rc,var(--c))}100%{box-shadow:0 0 0 26px transparent}}
.val{font-size:29px;font-weight:800}.vpd .val{font-size:40px}.lbl{font-size:11px;color:#94A3B8;margin-top:4px;letter-spacing:.08em}
.hydro{display:flex;justify-content:center;gap:14px;padding:8px 0;border-top:1px solid #1E293B;border-bottom:1px solid #1E293B}
.pill{background:#0F172A;border:1px solid #1E293B;border-radius:99px;padding:4px 14px;font-size:13px;font-weight:700}.ports{display:flex;justify-content:space-around}
.pt{display:flex;flex-direction:column;align-items:center;font-size:11px;color:#94A3B8}.pval{font-size:14px;font-weight:800;color:#F8FAFC;margin-bottom:4px}
.pcirc{width:46px;height:46px;border-radius:50%;border:2px solid #334155;display:flex;align-items:center;justify-content:center;color:#64748B}
.pcirc svg{width:22px;height:22px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
.on .pcirc{border-color:var(--c);color:var(--c);box-shadow:0 0 12px var(--c)}.spin{animation:sp var(--spd,2s) linear infinite}@keyframes sp{to{transform:rotate(360deg)}}
</style></head><body>
<div class="col" style="--c:#34D399"><div class="hdr"><span class="dot" onclick="App.tapDot()"></span><span style="color:#34D399">FLOWER AI+</span></div><svg class="dial" id="fd" viewBox="0 0 64 64"></svg>
<div class="rings"><div class="ring" id="ftr"><div class="val" id="ft">--°C</div><div class="lbl">TEMP</div></div><div class="ring vpd" id="fvr"><div class="val" id="fv">--</div><div class="lbl">VPD kPa</div></div><div class="ring" id="fhr"><div class="val" id="fh">--%</div><div class="lbl">RH</div></div></div>
<div class="hydro"><span class="pill" id="ph">pH --</span><span class="pill" id="ec">EC --</span><span class="pill" id="wt">WATER --°C</span></div><div class="ports" id="fp"></div></div>
<div class="col" style="--c:#60A5FA"><div class="hdr"><span style="color:#60A5FA">VEG 69+</span></div><svg class="dial" id="vd" viewBox="0 0 64 64"></svg>
<div class="rings"><div class="ring" id="vtr"><div class="val" id="vt">--°C</div><div class="lbl">TEMP</div></div><div class="ring vpd" id="vvr"><div class="val" id="vv">--</div><div class="lbl">VPD kPa</div></div><div class="ring" id="vhr"><div class="val" id="vh">--%</div><div class="lbl">RH</div></div></div>
<div class="hydro" style="visibility:hidden"><span class="pill">--</span></div><div class="ports" id="vp"></div></div>
<script>
const S={fan:'<circle cx="12" cy="12" r="2"/><path d="M12 10c0-3 2-6 4.5-5S18 9 14 11m-1 2.5c2.5 1.5 4 4.5 2 6s-4.5-.5-4-4.5m-2 0c-2.5 1.5-6 1.5-6-1s3-3.5 6-1"/>',bulb:'<path d="M9 18h6m-5 3h4M9 14c-2-1.5-3-3.5-3-6a6 6 0 1 1 12 0c0 2.5-1 4.5-3 6v1H9v-1z"/>',heat:'<path d="M12 2c2 4 6 6.5 6 11a6 6 0 1 1-12 0c0-2.5 1.5-4.5 3-6-.5 2 1 3 2 3 0-3 1-5.5 1-8z"/>',dehum:'<path d="M12 3l5.5 6.5a7.5 7.5 0 1 1-11 0L12 3z"/><path d="M12 10v6m-2.5-2.5L12 16l2.5-2.5"/>',hum:'<path d="M12 3l5.5 6.5a7.5 7.5 0 1 1-11 0L12 3z"/><path d="M9.5 16c1-1.5 4-1.5 5 0M10.5 13.5c.8-1 2.2-1 3 0"/>'};
function svgFor(n){return n.includes('DEHUM')?S.dehum:n.includes('HUMID')?S.hum:n.includes('FAN')?S.fan:n.includes('HEAT')?S.heat:n.includes('LIGHT')?S.bulb:'<circle cx="12" cy="12" r="5"/>';}
function tint(el,v,lo,hi,ok){const n=parseFloat(v);if(!isNaN(n)){const c=n<lo||n>hi?'#F87171':n<lo+.15||n>hi-.15?'#FBBF24':ok;el.style.setProperty('--rc',c);el.style.setProperty('--bs',c===ok?'4s':'1.8s');}}
function dial(id,onM,offM,c){const el=document.getElementById(id);if(!el)return;if(onM==null||onM<0||offM<0){el.innerHTML='';return;}const now=new Date(),cur=now.getHours()*60+now.getMinutes(),isOn=onM<offM?(cur>=onM&&cur<offM):(cur>=onM||cur<offM),nxt=isOn?offM:onM,rem=(nxt-cur+1440)%1440,dur=(offM-onM+1440)%1440,pt=(m,r)=>{const a=(m/1440)*2*Math.PI-Math.PI/2;return[32+r*Math.cos(a),32+r*Math.sin(a)];},[x1,y1]=pt(onM,26),[x2,y2]=pt(offM,26),[nx,ny]=pt(cur,23);el.innerHTML='<path d="M32 2v4M62 32h-4M32 62v-4M2 32h4" stroke="#475569" stroke-width="1.3" stroke-linecap="round"/><circle cx="32" cy="32" r="26" fill="none" stroke="#1E293B" stroke-width="5"/><path d="M '+x1.toFixed(1)+' '+y1.toFixed(1)+' A 26 26 0 '+(dur>720?1:0)+' 1 '+x2.toFixed(1)+' '+y2.toFixed(1)+'" fill="none" stroke="'+c+'" stroke-width="5" stroke-linecap="round"/><circle cx="'+x1.toFixed(1)+'" cy="'+y1.toFixed(1)+'" r="2.6" fill="#F8FAFC"/><circle cx="'+x2.toFixed(1)+'" cy="'+y2.toFixed(1)+'" r="2.6" fill="#050608" stroke="'+c+'" stroke-width="1.5"/><line x1="32" y1="32" x2="'+nx.toFixed(1)+'" y2="'+ny.toFixed(1)+'" stroke="#FACC15" stroke-width="1.8" stroke-linecap="round"/><circle cx="32" cy="32" r="16" fill="#050608"/><text x="32" y="31" text-anchor="middle" fill="#F8FAFC" font-size="9.5" font-weight="800">'+Math.floor(rem/60)+':'+String(rem%60).padStart(2,'0')+'</text><text x="32" y="40" text-anchor="middle" fill="#94A3B8" font-size="6.5" font-weight="700">'+(isOn?'TO OFF':'TO ON')+'</text>';}
function ports(id,list){document.getElementById(id).innerHTML=list.map(p=>'<div class="pt '+(p.on?'on':'')+'"><div class="pval">'+p.v+'</div><div class="pcirc"><svg class="'+(p.n.includes('FAN')&&+p.v>0?'spin" style="--spd:'+Math.max(.3,2.2-p.v*.18)+'s':'')+'" viewBox="0 0 24 24">'+svgFor(p.n)+'</svg></div><div style="margin-top:4px">'+p.n+'</div></div>').join('');}
function render(d){[['f','#34D399',18,28,1.0,1.5,40,65],['v','#60A5FA',20,30,0.8,1.2,55,78]].forEach(([k,ok,t1,t2,v1,v2,h1,h2])=>{if(!d[k])return;document.getElementById(k+'t').textContent=d[k].t;document.getElementById(k+'v').textContent=d[k].v;document.getElementById(k+'h').textContent=d[k].h;tint(document.getElementById(k+'tr'),d[k].t,t1,t2,ok);tint(document.getElementById(k+'vr'),d[k].v,v1,v2,ok);tint(document.getElementById(k+'hr'),d[k].h,h1,h2,ok);const vr=document.getElementById(k+'vr');vr.classList.remove('ripple');void vr.offsetWidth;vr.classList.add('ripple');ports(k+'p',d[k].p);dial(k+'d',d[k].on,d[k].off,ok);});if(d.f){document.getElementById('ph').textContent='pH '+d.f.ph;document.getElementById('ec').textContent='EC '+d.f.ec;document.getElementById('wt').textContent='WATER '+d.f.wt;}}
</script></body></html>"""
}
