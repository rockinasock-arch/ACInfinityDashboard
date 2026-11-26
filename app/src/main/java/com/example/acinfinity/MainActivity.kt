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
    private var userId: String? = null
    private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val prefs = getSharedPreferences("aci_prefs", Context.MODE_PRIVATE)
        token = prefs.getString("token", null)
        userId = prefs.getString("userId", null)

        if (token != null && userId != null) {
            showDashboard()
        } else {
            showLogin()
        }
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0A0B0E"))
            setPadding(120, 60, 120, 60)
        }
        val title = TextView(this).apply {
            text = "AC Infinity Wall Dashboard"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val emailInput = EditText(this).apply {
            hint = "AC Infinity Email"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passInput = EditText(this).apply {
            hint = "Password"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val status = TextView(this).apply {
            setTextColor(Color.parseColor("#F43F85"))
            gravity = Gravity.CENTER
        }
        val loginBtn = Button(this).apply {
            text = "Connect Controllers"
            setBackgroundColor(Color.parseColor("#F43F85"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                status.text = "Connecting..."
                login(emailInput.text.toString().trim(), passInput.text.toString()) { ok, msg ->
                    handler.post {
                        if (ok) showDashboard() else status.text = msg
                    }
                }
            }
        }
        root.addView(title)
        root.addView(emailInput)
        root.addView(passInput)
        root.addView(loginBtn)
        root.addView(status)
        setContentView(root)
    }

    private fun showDashboard() {
        dashboardView = DashboardView(this) {
            getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            polling = false
            showLogin()
        }
        setContentView(dashboardView)
        polling = true
        pollLoop()
    }

    private fun pollLoop() {
        if (!polling) return
        fetchDevices()
        handler.postDelayed({ pollLoop() }, 20000)
    }

    private fun login(email: String, pass: String, cb: (Boolean, String) -> Unit) {
        thread {
            try {
                val url = URL("http://acinfinityserver.com")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                }
                val body = "appEmail=${URLEncoder.encode(email, "UTF-8")}&appPasswordl=${URLEncoder.encode(pass, "UTF-8")}"
                conn.outputStream.use { it.write(body.toByteArray()) }
                val res = JSONObject(conn.inputStream.bufferedReader().readText())
                if (res.optInt("code") == 200) {
                    val data = res.getJSONObject("data")
                    token = data.getString("appId")
                    userId = data.getString("appId")
                    getSharedPreferences("aci_prefs", Context.MODE_PRIVATE).edit()
                        .putString("token", token).putString("userId", userId).apply()
                    cb(true, "Connected")
                } else {
                    cb(false, res.optString("msg", "Login failed"))
                }
            } catch (e: Exception) {
                cb(false, "Error: ${e.message}")
            }
        }
    }

    private fun fetchDevices() {
        val tk = token ?: return
        thread {
            try {
                val url = URL("http://acinfinityserver.com")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    setRequestProperty("token", tk)
                }
                val body = "userId=${URLEncoder.encode(tk, "UTF-8")}"
                conn.outputStream.use { it.write(body.toByteArray()) }
                val res = JSONObject(conn.inputStream.bufferedReader().readText())
                if (res.optInt("code") == 200) {
                    val arr = res.getJSONArray("data")
                    val list = mutableListOf<ControllerData>()
                    for (i in 0 until arr.length()) {
                        val d = arr.getJSONObject(i)
                        val info = d.optJSONObject("deviceInfo") ?: d
                        val temp = info.optDouble("temperature", 2500.0) / 100.0
                        val rh = info.optDouble("humidity", 5500.0) / 100.0
                        val vpd = info.optDouble("vpdnums", 120.0) / 100.0
                        val portsArr = info.optJSONArray("ports")
                        val ports = mutableListOf<PortData>()
                        if (portsArr != null) {
                            for (p in 0 until portsArr.length()) {
                                val po = portsArr.getJSONObject(p)
                                val online = po.optInt("online", 1) == 1 && po.optInt("loadType", 0) > 0
                                ports.add(PortData(
                                    po.optInt("port", p + 1),
                                    po.optString("portName", "Port ${p + 1}"),
                                    po.optInt("speak", 0),
                                    po.optInt("loadType", 1),
                                    online
                                ))
                            }
                        }
                        var ph: Double? = null
                        var ec: Double? = null
                        var wTemp: Double? = null
                        val sensors = info.optJSONArray("sensors")
                        if (sensors != null) {
                            for (s in 0 until sensors.length()) {
                                val so = sensors.getJSONObject(s)
                                when (so.optInt("sensorType")) {
                                    11 -> ph = so.optDouble("sensorData", 580.0) / 100.0
                                    12 -> ec = so.optDouble("sensorData", 180.0) / 100.0
                                    13 -> wTemp = so.optDouble("sensorData", 1940.0) / 100.0
                                }
                            }
                        }
                        list.add(ControllerData(
                            d.optString("devId", "$i"),
                            d.optString("devName", if (d.optInt("devType") == 20) "AI+" else "69 Pro"),
                            d.optInt("devType", 11),
                            temp, rh, vpd,
                            info.optInt("tTrend", 1), info.optInt("hTrend", 1),
                            ph, ec, wTemp, ports
                        ))
                    }
                    val sorted = list.sortedByDescending { it.type == 20 }
                    handler.post { dashboardView.updateData(sorted) }
                }
            } catch (_: Exception) {}
        }
    }
}
