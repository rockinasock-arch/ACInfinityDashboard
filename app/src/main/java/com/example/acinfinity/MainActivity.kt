package com.example.acinfinity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

data class PortData(val id: Int, val name: String, val speed: Int, val loadType: Int, val online: Boolean)
data class ControllerData(
    val id: String, val name: String, val type: Int,
    val tempC: Double, val rh: Double, val vpd: Double,
    val tempTrend: Int, val rhTrend: Int,
    val ph: Double?, val ec: Double?, val waterTempC: Double?,
    val ports: List<PortData>
)

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var dashboardView: DashboardView
    private var token: String? = null
    private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        token = getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).getString("token", null)
        if (token != null) showDashboard() else showLogin()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0A0B0E")); setPadding(120, 40, 120, 40)
        }
        val title = TextView(this).apply { text = "AC Infinity Wall Dashboard"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val emailInput = EditText(this).apply { hint = "AC Infinity Email"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS }
        val passInput = EditText(this).apply { hint = "Password"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val status = TextView(this).apply { setTextColor(Color.parseColor("#F43F85")); gravity = Gravity.CENTER }
        val btn = Button(this).apply {
            text = "Connect Controllers"; setBackgroundColor(Color.parseColor("#F43F85")); setTextColor(Color.WHITE)
            setOnClickListener {
                status.text = "Connecting..."
                login(emailInput.text.toString().trim(), passInput.text.toString()) { ok, msg ->
                    handler.post { if (ok) showDashboard() else status.text = msg }
                }
            }
        }
        listOf(title, emailInput, passInput, btn, status).forEach { root.addView(it) }
        setContentView(root)
    }

    private fun showDashboard() {
        dashboardView = DashboardView(this) {
            getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            polling = false; showLogin()
        }
        setContentView(dashboardView); polling = true; pollLoop()
    }

    private fun pollLoop() {
        if (!polling) return
        fetchDevices()
        handler.postDelayed({ pollLoop() }, 20000)
    }

    private fun apiPost(path: String, body: String, tk: String? = null): String {
        val host = listOf("www", "acinfinityserver", "com").joinToString(".")
        val conn = (URL("http", host, path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 10000; readTimeout = 10000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "ACInfinity/1.9.7 (Android)")
            if (tk != null) setRequestProperty("token", tk)
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return stream.bufferedReader().readText().trim()
    }

    private fun login(email: String, pass: String, cb: (Boolean, String) -> Unit) {
        thread {
            try {
                val safePass = if (pass.length > 25) pass.substring(0, 25) else pass
                val raw = apiPost("/api/user/appUserLogin", "appEmail=${URLEncoder.encode(email, "UTF-8")}&appPasswordl=${URLEncoder.encode(safePass, "UTF-8")}")
                if (!raw.startsWith("{")) { cb(false, "Server reply: ${raw.take(120)}"); return@thread }
                val res = JSONObject(raw)
                if (res.optInt("code") == 200) {
                    token = res.getJSONObject("data").getString("appId")
                    getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).edit().putString("token", token).apply()
                    cb(true, "Connected")
                } else cb(false, "API ${res.optInt("code")}: ${res.optString("msg", raw)}")
            } catch (e: Exception) { cb(false, "Error: ${e.javaClass.simpleName} - ${e.message}") }
        }
    }

    private fun fetchDevices() {
        val tk = token ?: return
        thread {
            try {
                val raw = apiPost("/api/user/devInfoListAll", "userId=${URLEncoder.encode(tk, "UTF-8")}", tk)
                val res = JSONObject(raw)
                if (res.optInt("code") != 200) return@thread
                val arr = res.getJSONArray("data")
                val list = (0 until arr.length()).map { i ->
                    val d = arr.getJSONObject(i)
                    val info = d.optJSONObject("deviceInfo") ?: d
                    val pArr = info.optJSONArray("ports")
                    val ports = if (pArr == null) emptyList() else (0 until pArr.length()).map { p ->
                        val po = pArr.getJSONObject(p)
                        PortData(po.optInt("port", p + 1), po.optString("portName", "P${p + 1}"), po.optInt("speak", 0), po.optInt("loadType", 1), po.optInt("online", 1) == 1 && po.optInt("loadType", 0) > 0)
                    }
                    var ph: Double? = null; var ec: Double? = null; var wt: Double? = null
                    val sArr = info.optJSONArray("sensors")
                    if (sArr != null) for (s in 0 until sArr.length()) {
                        val so = sArr.getJSONObject(s)
                        when (so.optInt("sensorType")) { 11 -> ph = so.optDouble("sensorData") / 100.0; 12 -> ec = so.optDouble("sensorData") / 100.0; 13 -> wt = so.optDouble("sensorData") / 100.0 }
                    }
                    ControllerData(d.optString("devId", "$i"), d.optString("devName", if (d.optInt("devType") == 20) "AI+" else "69 Pro"), d.optInt("devType", 11), info.optDouble("temperature", 2500.0) / 100.0, info.optDouble("humidity", 5500.0) / 100.0, info.optDouble("vpdnums", 120.0) / 100.0, info.optInt("tTrend", 1), info.optInt("hTrend", 1), ph, ec, wt, ports)
                }
                handler.post { dashboardView.updateData(list.sortedByDescending { it.type == 20 }) }
            } catch (_: Exception) {}
        }
    }
}
