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
                    val nm = it.name.uppercase(Locale.US).let { s -> if (s == "DEHUMIDIFIER") "DEHUM" else s }
                    put(JSONObject().put("n", nm).put("v", it.valueText).put("on", it.isOn))
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
body{background:#050608 radial-gradient(#1E293B 1px,transparent 1px);background-size:22px 22px;animation:gd 60s linear infinite;color:#F8FAFC;height:100vh;display:flex;overflow:hidden}
@keyframes gd{from{background-position:0 0}to{background-position:220px 220px}}
.clk{position:fixed;bottom:48px;left:50%;transform:translateX(-50%);z-index:10;background:#050608EB;border:1px solid #1E293B;border-radius:14px;padding:6px 15px;text-align:center;box-shadow:0 4px 18px #00000090}
.ctm{font-size:26px;font-weight:800;letter-spacing:.05em;color:#F8FAFC;line-height:1.05}
.cdt{font-size:11px;font-weight:700;letter-spacing:.09em;color:#94A3B8;margin-top:3px}
.col{width:50%;padding:28px 14px 48px;display:flex;flex-direction:column;justify-content:space-between;position:relative;overflow:hidden}
.col:first-child{border-right:1px solid #1E293B}
.glow{position:absolute;inset:0;pointer-events:none;background:radial-gradient(circle at 50% 50%,var(--c),transparent 68%);opacity:.14;animation:gl 9s ease-in-out infinite}
@keyframes gl{0%,100%{opacity:.11;transform:scale(.96)}50%{opacity:.22;transform:scale(1.05)}}
.snr{position:absolute;top:50%;left:50%;width:140px;height:140px;margin:-70px 0 0 -70px;border-radius:50%;border:1.5px solid var(--c);opacity:0;pointer-events:none}
.ping .snr{animation:sn 2.6s ease-out forwards}
@keyframes sn{0%{opacity:.45;transform:scale(1)}100%{opacity:0;transform:scale(3.6)}}
.sun{position:absolute;inset:0;background:linear-gradient(0deg,#F59E0B33 0%,#FACC1518 45%,transparent 85%);opacity:0;pointer-events:none}
.sunrise .sun{animation:sr 4.5s ease-out forwards}
@keyframes sr{0%{opacity:0;transform:translateY(40%)}30%{opacity:1;transform:translateY(0)}100%{opacity:0;transform:translateY(-15%)}}
.e420 .dial{filter:drop-shadow(0 0 10px #FACC15);animation:p4 1.6s ease-in-out infinite}
@keyframes p4{0%,100%{transform:scale(1)}50%{transform:scale(1.06)}}
.sparks{position:absolute;inset:0;pointer-events:none;overflow:hidden}
.sparks i{position:absolute;bottom:18%;width:5px;height:5px;border-radius:50%;background:#34D399;box-shadow:0 0 8px #FACC15;animation:fl 3.2s ease-out infinite}
@keyframes fl{0%{opacity:0;transform:translateY(0) scale(.6)}25%{opacity:.95}100%{opacity:0;transform:translateY(-140px) scale(1.2)}}
.hdr{display:flex;align-items:center;font-size:14px;font-weight:800;letter-spacing:.07em;z-index:2}
.dot{width:10px;height:10px;border-radius:50%;background:#34D399;display:inline-block;margin-right:7px}
.hyd{position:absolute;top:52px;left:14px;display:flex;gap:12px}
.mring{position:relative;flex-shrink:0;width:66px;height:66px;border-radius:50%;border:2px solid var(--rc,#34D399);box-shadow:0 0 11px -2px var(--rc,#34D399);background:#05060880;display:flex;flex-direction:column;align-items:center;justify-content:center}
.mval{font-size:15px;font-weight:800;color:#F8FAFC}
.mlbl{font-size:9px;color:#94A3B8;margin-top:2px;letter-spacing:.06em}
.dial{position:absolute;top:26px;right:12px;width:118px;height:118px}
.rings{display:flex;justify-content:space-around;align-items:center;padding:0 2px;margin-top:46px}
.ring{position:relative;flex-shrink:0;width:23vmin;height:23vmin;max-width:132px;max-height:132px;border-radius:50%;border:2.5px solid var(--rc,var(--c));box-shadow:0 0 14px -2px var(--rc,var(--c));background:#05060866;display:flex;flex-direction:column;align-items:center;justify-content:center;transition:border-color .6s,box-shadow .6s}
.ring.vpd{width:29vmin;height:29vmin;max-width:162px;max-height:162px;border-width:3.5px;animation:br var(--bs,4s) ease-in-out infinite}
.ripple{animation:rp .9s ease-out!important}
.drip{position:absolute;inset:0;width:100%;height:100%;pointer-events:none;opacity:0;transition:opacity .6s}
.up .drip,.dn .drip{opacity:1;animation:dp 3.4s ease-in-out infinite}
.dn .drip{transform:rotate(180deg)}
@keyframes dp{0%,100%{transform:scale(1) rotate(var(--rot,0deg))}50%{transform:scale(1.04) rotate(var(--rot,0deg))}}
.dn{--rot:180deg}
@keyframes br{0%,100%{box-shadow:0 0 12px -2px var(--rc,var(--c))}50%{box-shadow:0 0 28px 3px var(--rc,var(--c))}}
@keyframes rp{0%{box-shadow:0 0 0 0 var(--rc,var(--c))}100%{box-shadow:0 0 0 26px transparent}}
.val{font-size:24px;font-weight:800}
.vpd .val{font-size:33px}
.lbl{font-size:10px;color:#94A3B8;margin-top:3px;letter-spacing:.08em}
.ports{display:flex;justify-content:center;gap:14px}
.pt{display:flex;flex-direction:column;align-items:center;min-width:56px;font-size:9.5px;font-weight:700;color:#94A3B8}
.pval{font-size:13.5px;font-weight:800;color:#F8FAFC;margin-bottom:3px}
.pcirc{width:44px;height:44px;border-radius:50%;border:2px solid #334155;background:#05060880;display:flex;align-items:center;justify-content:center;color:#64748B}
.pcirc svg{width:20px;height:20px;stroke:currentColor;fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}
.on .pcirc{border-color:var(--c);color:var(--c);box-shadow:0 0 12px var(--c)}
.spin{animation:sp var(--spd,2s) linear infinite}
@keyframes sp{to{transform:rotate(360deg)}}
</style>
</head>
<body>
<div class="clk"><div class="ctm" id="ctm">--:--</div><div class="cdt" id="cdt">--- -- ---</div></div>
<div class="col" id="fc" style="--c:#34D399">
  <div class="glow"></div><div class="snr"></div><div class="sun"></div><div class="sparks" id="fsp"></div>
  <div class="hdr"><span class="dot" onclick="App.tapDot()"></span><span style="color:#34D399">FLOWER AI+</span></div>
  <div class="hyd">
    <div class="mring" id="phr"><div class="mval" id="ph">--</div><div class="mlbl">pH</div></div>
    <div class="mring" id="ecr"><div class="mval" id="ec">--</div><div class="mlbl">EC</div></div>
    <div class="mring" id="wtr"><div class="mval" id="wt">--</div><div class="mlbl">TEMP</div></div>
  </div>
  <svg class="dial" id="fd" viewBox="0 0 64 64"></svg>
  <div class="rings">
    <div class="ring" id="ftr"><div class="val" id="ft">--°C</div><div class="lbl">TEMP</div></div>
    <div class="ring vpd" id="fvr"><div class="val" id="fv">--</div><div class="lbl">VPD kPa</div></div>
    <div class="ring" id="fhr"><div class="val" id="fh">--%</div><div class="lbl">RH</div></div>
  </div>
  <div class="ports" id="fp"></div>
</div>
<div class="col" id="vc" style="--c:#60A5FA">
  <div class="glow"></div><div class="snr"></div><div class="sun"></div><div class="sparks" id="vsp"></div>
  <div class="hdr"><span style="color:#60A5FA">VEG 69+</span></div>
  <svg class="dial" id="vd" viewBox="0 0 64 64"></svg>
  <div class="rings">
    <div class="ring" id="vtr"><div class="val" id="vt">--°C</div><div class="lbl">TEMP</div></div>
    <div class="ring vpd" id="vvr"><div class="val" id="vv">--</div><div class="lbl">VPD kPa</div></div>
    <div class="ring" id="vhr"><div class="val" id="vh">--%</div><div class="lbl">RH</div></div>
  </div>
  <div class="ports" id="vp"></div>
</div>
<script>
const DAYS = ['SUN','MON','TUE','WED','THU','FRI','SAT'], MONS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
function tick() {
  const n = new Date();
  document.getElementById('ctm').textContent = String(n.getHours()).padStart(2,'0') + ':' + String(n.getMinutes()).padStart(2,'0');
  document.getElementById('cdt').textContent = DAYS[n.getDay()] + ' ' + n.getDate() + ' ' + MONS[n.getMonth()];
}
tick(); setInterval(tick, 1000);
const S = {
  fan: '<circle cx="12" cy="12" r="2"/><path d="M12 10c0-3 2-6 4.5-5S18 9 14 11m-1 2.5c2.5 1.5 4 4.5 2 6s-4.5-.5-4-4.5m-2 0c-2.5 1.5-6 1.5-6-1s3-3.5 6-1"/>',
  bulb: '<path d="M9 18h6m-5 3h4M9 14c-2-1.5-3-3.5-3-6a6 6 0 1 1 12 0c0 2.5-1 4.5-3 6v1H9v-1z"/>',
  heat: '<path d="M12 2c2 4 6 6.5 6 11a6 6 0 1 1-12 0c0-2.5 1.5-4.5 3-6-.5 2 1 3 2 3 0-3 1-5.5 1-8z"/>',
  dehum: '<path d="M12 3l5.5 6.5a7.5 7.5 0 1 1-11 0L12 3z"/><path d="M12 10v6m-2.5-2.5L12 16l2.5-2.5"/>',
  hum: '<path d="M12 3l5.5 6.5a7.5 7.5 0 1 1-11 0L12 3z"/><path d="M9.5 16c1-1.5 4-1.5 5 0M10.5 13.5c.8-1 2.2-1 3 0"/>'
};
const hist = {}, prevOn = {};
document.querySelectorAll('.ring,.mring').forEach(el => {
  el.insertAdjacentHTML('beforeend', '<svg class="drip" viewBox="0 0 100 100"><path d="M 30 5 Q 50 16 70 5 A 48 48 0 0 0 30 5 Z" fill="var(--rc,var(--c,#34D399))" opacity="0.85"/></svg>');
});
function trend(el, id, val, eps) {
  const n = parseFloat(val);
  if (isNaN(n)) return;
  const h = hist[id] || (hist[id] = []);
  h.push(n);
  if (h.length > 20) h.shift();
  const avg = h.reduce((a, b) => a + b, 0) / h.length;
  el.classList.remove('up', 'dn');
  if (h.length >= 3) {
    if (n - avg > eps) el.classList.add('up');
    else if (avg - n > eps) el.classList.add('dn');
  }
}
function svgFor(n) {
  if (n.includes('DEHUM')) return S.dehum;
  if (n.includes('HUMID')) return S.hum;
  if (n.includes('FAN')) return S.fan;
  if (n.includes('HEAT')) return S.heat;
  if (n.includes('LIGHT')) return S.bulb;
  return '<circle cx="12" cy="12" r="5"/>';
}
function tint(el, v, lo, hi, ok) {
  const n = parseFloat(v);
  if (isNaN(n)) return;
  const c = n < lo || n > hi ? '#F87171' : n < lo + .15 || n > hi - .15 ? '#FBBF24' : ok;
  el.style.setProperty('--rc', c);
  el.style.setProperty('--bs', c === ok ? '4s' : '1.8s');
}
function dial(k, onM, offM, c) {
  const el = document.getElementById(k + 'd'), col = document.getElementById(k + 'c'), sp = document.getElementById(k + 'sp');
  if (!el || onM == null || onM < 0 || offM < 0) { if (el) el.innerHTML = ''; return; }
  const now = new Date(), cur = now.getHours() * 60 + now.getMinutes();
  const isOn = onM < offM ? (cur >= onM && cur < offM) : (cur >= onM || cur < offM);
  if (prevOn[k] === false && isOn && col) { col.classList.remove('sunrise'); void col.offsetWidth; col.classList.add('sunrise'); }
  prevOn[k] = isOn;
  const is420 = (now.getHours() % 12 === 4) && now.getMinutes() === 20;
  if (col) col.classList.toggle('e420', is420);
  if (sp) sp.innerHTML = is420 ? '<i style="left:22%;animation-delay:0s"></i><i style="left:50%;animation-delay:.8s"></i><i style="left:76%;animation-delay:1.5s"></i>' : '';
  const nxt = isOn ? offM : onM, rem = (nxt - cur + 1440) % 1440, dur = (offM - onM + 1440) % 1440;
  const pt = (m, r) => { const a = (m / 1440) * 2 * Math.PI - Math.PI / 2; return [32 + r * Math.cos(a), 32 + r * Math.sin(a)]; };
  const [x1, y1] = pt(onM, 26), [x2, y2] = pt(offM, 26), [nx, ny] = pt(cur, 23);
  const ico = isOn
    ? '<circle cx="32" cy="19.5" r="2.2" fill="#FACC15"/><path d="M32 15.5v1.3M32 22.2v1.3M28 19.5h1.3M34.7 19.5H36M29.2 16.7l.9.9M33.9 21.4l.9.9M34.8 16.7l-.9.9M30.1 21.4l-.9.9" stroke="#FACC15" stroke-width="1.1" stroke-linecap="round"/>'
    : '<path d="M33.6 17.5a3.2 3.2 0 1 0 2.9 4.5 2.7 2.7 0 0 1-2.9-4.5z" fill="#93C5FD"/>';
  el.innerHTML = '<path d="M32 2v4M62 32h-4M32 62v-4M2 32h4" stroke="#475569" stroke-width="1.3" stroke-linecap="round"/>'
    + '<circle cx="32" cy="32" r="26" fill="none" stroke="#1E293B" stroke-width="5"/>'
    + '<path d="M ' + x1.toFixed(1) + ' ' + y1.toFixed(1) + ' A 26 26 0 ' + (dur > 720 ? 1 : 0) + ' 1 ' + x2.toFixed(1) + ' ' + y2.toFixed(1) + '" fill="none" stroke="' + (is420 ? '#FACC15' : c) + '" stroke-width="5" stroke-linecap="round"/>'
    + '<circle cx="' + x1.toFixed(1) + '" cy="' + y1.toFixed(1) + '" r="2.6" fill="#F8FAFC"/>'
    + '<circle cx="' + x2.toFixed(1) + '" cy="' + y2.toFixed(1) + '" r="2.6" fill="#050608" stroke="' + c + '" stroke-width="1.5"/>'
    + '<line x1="32" y1="32" x2="' + nx.toFixed(1) + '" y2="' + ny.toFixed(1) + '" stroke="#FACC15" stroke-width="1.8" stroke-linecap="round"/>'
    + '<circle cx="32" cy="32" r="16.5" fill="#050608"/>' + ico
    + '<text x="32" y="33.5" text-anchor="middle" fill="#F8FAFC" font-size="9" font-weight="800">' + Math.floor(rem / 60) + ':' + String(rem % 60).padStart(2, '0') + '</text>'
    + '<text x="32" y="41.5" text-anchor="middle" fill="#94A3B8" font-size="6" font-weight="700">' + (isOn ? 'TO OFF' : 'TO ON') + '</text>';
}
function ports(id, list) {
  document.getElementById(id).innerHTML = list.map(p => {
    const sp = p.n.includes('FAN') && +p.v > 0 ? 'spin" style="--spd:' + Math.max(.3, 2.2 - p.v * .18) + 's' : '';
    return '<div class="pt ' + (p.on ? 'on' : '') + '"><div class="pval">' + p.v + '</div><div class="pcirc"><svg class="' + sp + '" viewBox="0 0 24 24">' + svgFor(p.n) + '</svg></div><div style="margin-top:4px">' + p.n + '</div></div>';
  }).join('');
}
function render(d) {
  [['f', '#34D399', 18, 28, 1.0, 1.5, 40, 65], ['v', '#60A5FA', 20, 30, 0.8, 1.2, 55, 78]].forEach(([k, ok, t1, t2, v1, v2, h1, h2]) => {
    if (!d[k]) return;
    document.getElementById(k + 't').textContent = d[k].t;
    document.getElementById(k + 'v').textContent = d[k].v;
    document.getElementById(k + 'h').textContent = d[k].h;
    const tr = document.getElementById(k + 'tr'), vr = document.getElementById(k + 'vr'), hr = document.getElementById(k + 'hr'), col = document.getElementById(k + 'c');
    tint(tr, d[k].t, t1, t2, ok); tint(vr, d[k].v, v1, v2, ok); tint(hr, d[k].h, h1, h2, ok);
    trend(tr, k + 't', d[k].t, 0.15); trend(vr, k + 'v', d[k].v, 0.02); trend(hr, k + 'h', d[k].h, 0.4);
    vr.classList.remove('ripple'); void vr.offsetWidth; vr.classList.add('ripple');
    if (col) { col.classList.remove('ping'); void col.offsetWidth; col.classList.add('ping'); }
    ports(k + 'p', d[k].p);
    dial(k, d[k].on, d[k].off, ok);
  });
  if (d.f) {
    document.getElementById('ph').textContent = d.f.ph;
    document.getElementById('ec').textContent = d.f.ec;
    document.getElementById('wt').textContent = d.f.wt;
    trend(document.getElementById('phr'), 'ph', d.f.ph, 0.04);
    trend(document.getElementById('ecr'), 'ec', d.f.ec, 0.03);
    trend(document.getElementById('wtr'), 'wt', d.f.wt, 0.15);
  }
}
</script>
</body>
</html>"""
}
