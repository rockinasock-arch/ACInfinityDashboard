package com.example.acinfinity

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

class DashboardView(context: Context, private val onLogout: () -> Unit) : WebView(context) {
    private var dotTaps = 0; private var lastTapMs = 0L

    init {
        setBackgroundColor(Color.parseColor("#0B0D11"))
        settings.javaScriptEnabled = true
        loadDataWithBaseURL(null, htmlPage(), "text/html", "UTF-8", null)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN && e.x < width * 0.15f && e.y < height * 0.20f) {
            val now = System.currentTimeMillis()
            dotTaps = if (now - lastTapMs < 1500L) dotTaps + 1 else 1
            lastTapMs = now
            if (dotTaps >= 7) { dotTaps = 0; onLogout() }
        }
        return super.onTouchEvent(e)
    }

    fun updateData(list: List<ControllerData>) {
        val arr = JSONArray()
        for (c in list) {
            val ports = JSONArray()
            for (p in c.ports) ports.put(JSONObject().put("name", p.name).put("val", p.valueText).put("on", p.isOn))
            arr.put(JSONObject().put("name", c.name).put("type", c.type).put("temp", c.tempC).put("rh", c.rh).put("vpd", c.vpd).put("tt", c.tempTrend).put("ht", c.rhTrend).put("ph", c.ph ?: -1).put("ec", c.ec ?: -1).put("wt", c.waterTempC ?: -1).put("ports", ports))
        }
        post { evaluateJavascript("window.renderData && renderData($arr);", null) }
    }

    private fun htmlPage() = """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
*{box-sizing:border-box;margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;user-select:none}
body{background:#0B0D11;color:#F8FAFC;height:100vh;width:100vw;overflow:hidden;display:flex;gap:1.4vw;padding:1.4vh 1.4vw}
.card{flex:1;background:linear-gradient(165deg,#13161E 0%,#0E1016 100%);border:1px solid rgba(255,255,255,.08);border-radius:28px;padding:1.6vh 1.6vw;display:flex;flex-direction:column;justify-content:space-between;box-shadow:0 16px 40px rgba(0,0,0,.6)}
.hdr{display:flex;align-items:center;justify-content:space-between}
.title{display:flex;align-items:center;gap:10px;font-size:clamp(18px,2.2vw,26px);font-weight:700;letter-spacing:.08em}
.dot{width:10px;height:10px;border-radius:50%;background:#10B981;box-shadow:0 0 12px #10B981;animation:pulse 2.4s infinite}
@keyframes pulse{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.45;transform:scale(.85)}}
.meta{display:flex;align-items:center;gap:12px;font-size:clamp(12px,1.2vw,15px);color:#64748B;font-weight:600}
.badge{padding:3px 10px;border-radius:99px;background:rgba(16,185,129,.12);border:1px solid rgba(16,185,129,.45);color:#10B981;font-size:12px;font-weight:800}
.rings{display:flex;align-items:center;justify-content:space-between;gap:.8vw;margin:auto 0}
.dial{position:relative;border-radius:50%;background:radial-gradient(circle at 50% 35%,#191E29,#0F1218);box-shadow:inset 0 2px 8px rgba(0,0,0,.7),0 1px 0 rgba(255,255,255,.06);display:flex;flex-direction:column;align-items:center;justify-content:center}
.dial.sm{width:clamp(125px,14.2vw,195px);height:clamp(125px,14.2vw,195px)}
.dial.lg{width:clamp(150px,17.2vw,235px);height:clamp(150px,17.2vw,235px);box-shadow:inset 0 2px 10px rgba(0,0,0,.75),0 0 28px rgba(16,185,129,.12)}
.dial svg{position:absolute;inset:0;width:100%;height:100%;transform:rotate(-90deg)}
.num{font-size:clamp(28px,3.8vw,52px);font-weight:700;letter-spacing:-.03em;line-height:1}
.lg .num{font-size:clamp(34px,4.5vw,62px)}
.sub{font-size:clamp(10px,1vw,13px);font-weight:700;color:#64748B;letter-spacing:.12em;margin-top:4px}
.up{color:#10B981;font-size:.65em}.dn{color:#38BDF8;font-size:.65em}
.hydro{display:flex;justify-content:center;gap:1.4vw;margin-top:-1vh}
.horb{width:clamp(74px,8.2vw,108px);height:clamp(74px,8.2vw,108px);border-radius:50%;background:#141822;border:1px solid rgba(16,185,129,.28);display:flex;flex-direction:column;align-items:center;justify-content:center;box-shadow:inset 0 2px 6px rgba(0,0,0,.6)}
.hval{font-size:clamp(18px,2.2vw,28px);font-weight:700;color:#F8FAFC;line-height:1.05}
.hlbl{font-size:clamp(9px,.95vw,11px);font-weight:700;color:#64748B;letter-spacing:.1em;margin-top:2px}
.ports{display:flex;justify-content:center;gap:1.3vw;padding-top:.4vh}
.port{width:clamp(76px,8.6vw,114px);height:clamp(76px,8.6vw,114px);border-radius:50%;background:#12161F;border:2px solid #222938;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:2px;transition:.3s}
.port.on{border-color:#10B981;background:radial-gradient(circle at 50% 30%,rgba(16,185,129,.16),#12161F 70%);box-shadow:0 0 20px rgba(16,185,129,.18)}
.port svg{width:28%;height:28%;stroke:#64748B;fill:none;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round}
.port.on svg{stroke:#10B981}
.port.on.fan svg{animation:spin 2s linear infinite}
@keyframes spin{100%{transform:rotate(360deg)}}
.pval{font-size:clamp(20px,2.5vw,32px);font-weight:700;line-height:1}
.plbl{font-size:clamp(8px,.85vw,10px);font-weight:700;color:#64748B;letter-spacing:.08em;max-width:82%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.sync{margin:auto;color:#64748B;font-size:20px;letter-spacing:.1em}
</style></head><body id="app"><div class="sync">SYNCING AC INFINITY CLOUD...</div>
<script>
function ico(n){n=n.toLowerCase();
if(n.includes('fan'))return'<svg viewBox="0 0 24 24"><path d="M12 12c0-3 2.5-5 5-5 2 0 3 1.5 3 3 0 2-2 2-8 2zm0 0c3 0 5 2.5 5 5 0 2-1.5 3-3 3-2 0-2-2-2-8zm0 0c0 3-2.5 5-5 5-2 0-3-1.5-3-3 0-2 2-2 8-2zm0 0c-3 0-5-2.5-5-5 0-2 1.5-3 3-3 2 0 2 2 2 8z"/></svg>';
if(n.includes('light'))return'<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41m11.32-11.32l1.41-1.41"/></svg>';
if(n.includes('heat'))return'<svg viewBox="0 0 24 24"><path d="M12 2c.5 4-2 6-2 10a4 4 0 0 0 8 0c0-3-2-5-2-8 2 1.5 4 4.5 4 8a8 8 0 1 1-16 0c0-4.5 3.5-7.5 8-10z"/></svg>';
return'<svg viewBox="0 0 24 24"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>';}
function ring(pct,col){const d=289,o=d-(d*Math.min(1,Math.max(.04,pct)));return'<svg viewBox="0 0 100 100"><circle cx="50" cy="50" r="46" stroke="#1C222E" stroke-width="5" fill="none"/><circle cx="50" cy="50" r="46" stroke="'+col+'" stroke-width="5" stroke-linecap="round" fill="none" stroke-dasharray="'+d+'" stroke-dashoffset="'+o+'"/></svg>';}
function renderData(list){if(!list||!list.length)return;
const clk=new Date().toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'});
document.getElementById('app').innerHTML=list.slice(0,2).map((c,idx)=>{
const ta=c.tt>0?'<span class="up">↑</span>':c.tt<0?'<span class="dn">↓</span>':'';
const ha=c.ht>0?'<span class="up">↑</span>':c.ht<0?'<span class="dn">↓</span>':'';
const hy=(c.ph>=0||c.ec>=0||c.wt>=0)?'<div class="hydro"><div class="horb"><div class="hval">'+(c.ph>=0?c.ph.toFixed(1):'--')+'</div><div class="hlbl">pH</div></div><div class="horb"><div class="hval">'+(c.ec>=0?c.ec.toFixed(2):'--')+'</div><div class="hlbl">EC mS</div></div><div class="horb"><div class="hval">'+(c.wt>=0?c.wt.toFixed(1)+'°':'--')+'</div><div class="hlbl">TEMP °C</div></div></div>':'';
const ps=c.ports.map(p=>'<div class="port '+(p.on?'on ':'')+(p.name.toLowerCase().includes('fan')?'fan':'')+'">'+ico(p.name)+'<div class="pval">'+p.val+'</div><div class="plbl">'+(p.name||'PORT').toUpperCase()+'</div></div>').join('');
return'<div class="card"><div class="hdr"><div class="title"><div class="dot"></div>'+c.name.toUpperCase()+'</div><div class="meta">'+(idx===1?'<span>'+clk+'</span>':'')+'<span class="badge">'+(c.type==20?'AI+':'69 PRO')+'</span></div></div><div class="rings"><div class="dial sm">'+ring(c.temp/40,'#10B981')+'<div class="num">'+c.temp.toFixed(1)+'°'+ta+'</div><div class="sub">TEMP °C</div></div><div class="dial lg">'+ring(c.vpd/2.2,'#10B981')+'<div class="num">'+c.vpd.toFixed(2)+'</div><div class="sub">VPD kPa</div></div><div class="dial sm">'+ring(c.rh/100,'#10B981')+'<div class="num">'+c.rh.toFixed(0)+'%'+ha+'</div><div class="sub">HUMIDITY</div></div></div>'+hy+'<div class="ports">'+ps+'</div></div>';
}).join('');}
</script></body></html>
    """.trimIndent()
}
