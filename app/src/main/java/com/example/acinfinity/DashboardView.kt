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
        setBackgroundColor(Color.parseColor("#0D0F12"))
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
            arr.put(JSONObject().put("name", c.name).put("type", c.type).put("temp", c.tempC).put("rh", c.rh).put("vpd", c.vpd).put("ph", c.ph ?: -1).put("ec", c.ec ?: -1).put("wt", c.waterTempC ?: -1).put("ports", ports))
        }
        post { evaluateJavascript("window.renderData && renderData($arr);", null) }
    }

    private fun htmlPage() = """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
*{box-sizing:border-box;margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;user-select:none}
body{background:#0D0F12;color:#F8FAFC;height:100vh;width:100vw;overflow:hidden;display:flex;flex-direction:column;padding:1.4vh 1.4vw;gap:1.2vh}
.top{display:flex;align-items:center;justify-content:space-between;background:#13161B;border:1px solid rgba(255,255,255,.06);border-radius:14px;padding:.9vh 1.4vw}
.top-l{display:flex;align-items:center;gap:10px;font-size:clamp(12px,1.3vw,16px);font-weight:700;letter-spacing:.08em}
.dot{width:9px;height:9px;border-radius:50%;background:#10B981;box-shadow:0 0 10px #10B981;animation:pulse 2.2s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:.4}}
.pill-sync{background:rgba(16,185,129,.12);border:1px solid rgba(16,185,129,.35);color:#10B981;font-size:10px;font-weight:800;padding:3px 8px;border-radius:6px;letter-spacing:.08em}
.clk{font-size:clamp(13px,1.4vw,17px);font-weight:700;color:#CBD5E1;letter-spacing:.04em}
.grid{flex:1;display:flex;gap:1.4vw;min-height:0}
.zone{flex:1;background:#13161B;border:1px solid rgba(255,255,255,.07);border-radius:20px;padding:1.6vh 1.4vw;display:flex;flex-direction:column;justify-content:space-between}
.zhdr{display:flex;align-items:center;justify-content:space-between;font-size:clamp(12px,1.25vw,15px);font-weight:700;color:#94A3B8;letter-spacing:.1em}
.zlive{color:#10B981;font-size:11px;display:flex;align-items:center;gap:6px}
.zlive i{width:6px;height:6px;border-radius:50%;background:#10B981;display:inline-block}
.rings{display:flex;justify-content:space-between;gap:1vw;margin:auto 0}
.ring-box{position:relative;flex:1;aspect-ratio:1/1;max-width:31.5%;border-radius:50%;background:#161A20;box-shadow:inset 0 2px 10px rgba(0,0,0,.65);display:flex;flex-direction:column;align-items:center;justify-content:center}
.ring-box svg{position:absolute;inset:0;width:100%;height:100%;transform:rotate(-90deg)}
.rval{font-size:clamp(22px,2.9vw,40px);font-weight:700;letter-spacing:-.02em;line-height:1.05}
.rlbl{font-size:clamp(9px,.95vw,11px);font-weight:700;color:#64748B;letter-spacing:.12em;margin-top:3px}
.hydro{display:flex;justify-content:center;gap:1vw}
.hpill{background:#181C23;border:1px solid rgba(255,255,255,.06);border-radius:10px;padding:.7vh 1.2vw;display:flex;align-items:baseline;gap:6px}
.hl{font-size:11px;font-weight:700;color:#64748B}.hv{font-size:clamp(15px,1.8vw,22px);font-weight:700}
.hv.ph{color:#EF4444}.hv.wt{color:#10B981}
.ports{display:flex;justify-content:center;gap:1vw;background:#101318;border:1px solid rgba(255,255,255,.05);border-radius:16px;padding:1.2vh 1vw}
.ptile{flex:1;max-width:23%;display:flex;flex-direction:column;align-items:center;gap:5px}
.pcircle{position:relative;width:clamp(46px,4.8vw,64px);height:clamp(46px,4.8vw,64px);border-radius:50%;background:#171B22;display:flex;align-items:center;justify-content:center}
.pcircle svg.arc{position:absolute;inset:0;width:100%;height:100%;transform:rotate(-90deg)}
.pcircle svg.ico{width:40%;height:40%;stroke:#64748B;fill:none;stroke-width:2.2;stroke-linecap:round;stroke-linejoin:round;z-index:1}
.ptile.on .pcircle svg.ico{stroke:#10B981}
.ptile.on.fan .pcircle svg.ico{animation:spin 2s linear infinite}
@keyframes spin{100%{transform:rotate(360deg)}}
.pval{font-size:clamp(15px,1.7vw,21px);font-weight:700;line-height:1}
.plbl{font-size:clamp(8px,.85vw,10px);font-weight:700;color:#64748B;letter-spacing:.08em;text-transform:uppercase;max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.sync{margin:auto;color:#64748B;font-size:18px;letter-spacing:.1em}
</style></head><body>
<div class="top"><div class="top-l"><div class="dot"></div>AC INFINITY WALL DASHBOARD<span class="pill-sync">● LIVE SYNCED</span></div><div class="clk" id="clk">--:--</div></div>
<div class="grid" id="app"><div class="sync">SYNCING AC INFINITY CLOUD...</div></div>
<script>
setInterval(()=>{document.getElementById('clk').textContent=new Date().toLocaleTimeString([],{hour:'numeric',minute:'2-digit'});},1000);
function ico(n){n=n.toLowerCase();
if(n.includes('fan'))return'<svg class="ico" viewBox="0 0 24 24"><path d="M12 12c0-3 2.5-5 5-5 2 0 3 1.5 3 3 0 2-2 2-8 2zm0 0c3 0 5 2.5 5 5 0 2-1.5 3-3 3-2 0-2-2-2-8zm0 0c0 3-2.5 5-5 5-2 0-3-1.5-3-3 0-2 2-2 8-2zm0 0c-3 0-5-2.5-5-5 0-2 1.5-3 3-3 2 0 2 2 2 8z"/></svg>';
if(n.includes('light'))return'<svg class="ico" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41m11.32-11.32l1.41-1.41"/></svg>';
if(n.includes('heat'))return'<svg class="ico" viewBox="0 0 24 24"><path d="M12 2c.5 4-2 6-2 10a4 4 0 0 0 8 0c0-3-2-5-2-8 2 1.5 4 4.5 4 8a8 8 0 1 1-16 0c0-4.5 3.5-7.5 8-10z"/></svg>';
return'<svg class="ico" viewBox="0 0 24 24"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>';}
function ring(pct,col,sw){const d=276,o=d-(d*Math.min(1,Math.max(.05,pct)));return'<svg class="arc" viewBox="0 0 100 100"><circle cx="50" cy="50" r="44" stroke="#222730" stroke-width="'+sw+'" fill="none"/><circle cx="50" cy="50" r="44" stroke="'+col+'" stroke-width="'+sw+'" stroke-linecap="round" fill="none" stroke-dasharray="'+d+'" stroke-dashoffset="'+o+'"/></svg>';}
function renderData(list){if(!list||!list.length)return;
document.getElementById('app').innerHTML=list.slice(0,2).map(c=>{
const vOk=c.type==20?(c.vpd>=0.8&&c.vpd<=1.5):(c.vpd>=0.6&&c.vpd<=1.2);
const hy=(c.ph>=0||c.ec>=0||c.wt>=0)?'<div class="hydro"><div class="hpill"><span class="hl">pH</span><span class="hv ph">'+(c.ph>=0?c.ph.toFixed(1):'--')+'</span></div><div class="hpill"><span class="hl">EC</span><span class="hv">'+(c.ec>=0?c.ec.toFixed(2):'--')+'</span></div><div class="hpill"><span class="hl">TEMP</span><span class="hv wt">'+(c.wt>=0?c.wt.toFixed(1)+'°C':'--')+'</span></div></div>':'';
const ps=c.ports.map(p=>{const num=p.val==='ON'?10:(parseInt(p.val)||0);return'<div class="ptile '+(p.on?'on ':'')+(p.name.toLowerCase().includes('fan')?'fan':'')+'"><div class="pcircle">'+ring(p.on?num/10:0,p.on?'#10B981':'#222730',6)+ico(p.name)+'</div><div class="pval">'+p.val+'</div><div class="plbl">'+(p.name||'PORT')+'</div></div>';}).join('');
return'<div class="zone"><div class="zhdr"><span>'+c.name.toUpperCase()+' ZONE</span><span class="zlive"><i></i>LIVE SYNCED</span></div><div class="rings"><div class="ring-box">'+ring(c.temp/35,'#EF4444',7)+'<div class="rval">'+c.temp.toFixed(1)+'°C</div><div class="rlbl">TEMP</div></div><div class="ring-box">'+ring(c.vpd/2.0,vOk?'#10B981':'#EF4444',7)+'<div class="rval">'+c.vpd.toFixed(2)+'</div><div class="rlbl">VPD kPa</div></div><div class="ring-box">'+ring(c.rh/100,'#10B981',7)+'<div class="rval">'+c.rh.toFixed(1)+'%</div><div class="rlbl">HUMIDITY</div></div></div>'+hy+'<div class="ports">'+ps+'</div></div>';
}).join('');}
</script></body></html>
    """.trimIndent()
}
