package com.example.acinfinity

import android.content.Context
import android.graphics.Color
import android.os.*
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.*
import kotlin.concurrent.thread
import kotlin.math.exp

data class PortData(val id: Int, val name: String, val valueText: String, val isOn: Boolean, val loadType: Int)
data class ControllerData(val id: String, val name: String, val type: Int, val tempC: Double, val rh: Double, val vpd: Double, val tempTrend: Int, val rhTrend: Int, val ph: Double?, val ec: Double?, val waterTempC: Double?, val ports: List<PortData>)

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dashboardView: DashboardView
    private var token: String? = null
    private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val prefs = getSharedPreferences("aci_prefs", Context.MODE_PRIVATE)
        token = prefs.getString("token", null)
        if (token != null || prefs.getString("email", null) != null) showDashboard() else showLogin()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(Color.parseColor("#0D0F12")); setPadding(120, 40, 120, 40) }
        val title = TextView(this).apply { text = "AC Infinity Wall Dashboard"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val email = EditText(this).apply { hint = "AC Infinity Email"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val pass = EditText(this).apply { hint = "Password"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val status = TextView(this).apply { setTextColor(Color.parseColor("#10B981")); gravity = Gravity.CENTER }
        val btn = Button(this).apply {
            text = "Connect Controllers"; setBackgroundColor(Color.parseColor("#10B981")); setTextColor(Color.WHITE)
            setOnClickListener { status.text = "Connecting..."; login(email.text.toString().trim(), pass.text.toString()) { ok, msg -> handler.post { if (ok) showDashboard() else status.text = msg } } }
        }
        listOf(title, email, pass, btn, status).forEach { root.addView(it) }
        setContentView(root)
    }

    private fun showDashboard() {
        dashboardView = DashboardView(this) { getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).edit().clear().apply(); token = null; polling = false; showLogin() }
        setContentView(dashboardView); polling = true; pollLoop()
    }

    private fun pollLoop() { if (!polling) return; fetchDevices(); handler.postDelayed({ pollLoop() }, 15000) }

    private fun apiPost(path: String, body: String, tk: String? = null): String {
        val host = listOf("www", "acinfinityserver", "com").joinToString(".")
        val conn = (URL("http", host, path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10000; readTimeout = 10000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); setRequestProperty("User-Agent", "ACInfinity/1.9.7 (Android)")
            if (tk != null) setRequestProperty("token", tk)
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        return (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream).bufferedReader().readText().trim()
    }

    private fun login(email: String, pass: String, cb: (Boolean, String) -> Unit) {
        thread {
            try {
                val p = if (pass.length > 25) pass.substring(0, 25) else pass
                val res = JSONObject(apiPost("/api/user/appUserLogin", "appEmail=${URLEncoder.encode(email, "UTF-8")}&appPasswordl=${URLEncoder.encode(p, "UTF-8")}"))
                if (res.optInt("code") == 200) {
                    token = res.getJSONObject("data").getString("appId")
                    getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).edit().putString("token", token).putString("email", email).putString("pass", pass).commit(); cb(true, "OK")
                } else cb(false, "API ${res.optInt("code")}: ${res.optString("msg")}")
            } catch (e: Exception) { cb(false, "Error: ${e.message}") }
        }
    }

    private fun fetchDevices() {
        val prefs = getSharedPreferences("aci_prefs", Context.MODE_PRIVATE)
        val tk = token ?: run { val e = prefs.getString("email", null); val p = prefs.getString("pass", null); if (e != null && p != null) login(e, p) { _, _ -> }; return }
        thread {
            try {
                val res = JSONObject(apiPost("/api/user/devInfoListAll", "userId=${URLEncoder.encode(tk, "UTF-8")}", tk))
                if (res.optInt("code") != 200) { val e = prefs.getString("email", null); val p = prefs.getString("pass", null); if (e != null && p != null) login(e, p) { _, _ -> }; return@thread }
                val arr = res.getJSONArray("data"); val list = mutableListOf<ControllerData>()
                for (i in 0 until arr.length()) {
                    val d = arr.getJSONObject(i); val info = d.optJSONObject("deviceInfo") ?: d; val devType = d.optInt("devType", 11)
                    var temp = info.optDouble("temperature", 2200.0) / 100.0; var rh = info.optDouble("humidity", 5500.0) / 100.0; var vpd = info.optDouble("vpdnums", 100.0) / 100.0
                    var ph: Double? = null; var ec: Double? = null; var wt: Double? = null; val sArr = info.optJSONArray("sensors")
                    if (sArr != null) {
                        var hasVpd = false
                        for (s in 0 until sArr.length()) {
                            val so = sArr.getJSONObject(s); val st = so.optInt("sensorType"); val sv = so.optDouble("sensorData", 0.0) / 100.0
                            when (st) {
                                1 -> if (sv > 0) temp = sv
                                2 -> if (sv > 0) rh = sv
                                3 -> if (sv > 0) { vpd = sv; hasVpd = true }
                                13 -> if (sv > 0) ph = sv
                                15 -> if (sv > 0) ec = if (sv > 20.0) sv / 10.0 else sv
                                16 -> if (sv > 0) wt = if (sv > 45.0) (sv - 32.0) * 5.0 / 9.0 else sv
                            }
                        }
                        if (devType == 20 && !hasVpd) vpd = (0.61078 * exp((17.27 * temp) / (temp + 237.3))) * (1.0 - rh / 100.0)
                    }
                    val pArr = info.optJSONArray("ports"); val ports = mutableListOf<PortData>()
                    if (pArr != null) for (p in 0 until pArr.length()) {
                        val po = pArr.getJSONObject(p); if (po.optInt("online", 0) != 1) continue
                        val speak = po.optInt("speak", 0); val lt = po.optInt("loadType", 1)
                        val isOut = lt == 4 || lt == 5 || lt == 6 || po.optString("portName").lowercase().let { it.contains("dehum") || it.contains("on/off") }
                        ports.add(PortData(po.optInt("port", p + 1), po.optString("portName", ""), if (isOut) (if (speak > 0) "ON" else "OFF") else "$speak", speak > 0, lt))
                    }
                    list.add(ControllerData(d.optString("devId", "$i"), d.optString("devName", "Controller"), devType, temp, rh, vpd, info.optInt("tTrend", 0), info.optInt("hTrend", 0), ph, ec, wt, ports))
                }
                val sorted = list.sortedByDescending { it.type == 20 }; handler.post { dashboardView.updateData(sorted) }
            } catch (_: Exception) {}
        }
    }
}
