package com.example.wlanscanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.ClipData
import android.content.ClipboardManager

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var listLayout: LinearLayout

    // الخريطة لتحويل SSID (معكوسة للتشفير البسيط)
    private val map = mapOf(
        '1' to 'e', 'e' to '1',
        '2' to 'd', 'd' to '2',
        '3' to 'c', 'c' to '3',
        '4' to 'b', 'b' to '4',
        '5' to 'a', 'a' to '5',
        '6' to '9', '9' to '6',
        '7' to '8', '8' to '7',
        'f' to '0', '0' to 'f'
    )

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val results = wifiManager.scanResults
            showResults(results)
        }
    }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startScanIfReady()
            } else {
                Toast.makeText(this, R.string.location_needed, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        listLayout = findViewById(R.id.list)

        findViewById<Button>(R.id.scanBtn).setOnClickListener {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
    }

    private fun startScanIfReady() {
        if (!checkLocationEnabled()) {
            promptEnableLocation()
            return
        }
        val ok = wifiManager.startScan()
        if (!ok) {
            Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.scanning, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun promptEnableLocation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.enable_location)
            .setMessage(R.string.location_message)
            .setPositiveButton("نعم") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showResults(results: List<ScanResult>) {
        listLayout.removeAllViews()
        if (results.isEmpty()) {
            val t = TextView(this)
            t.text = getString(R.string.no_networks)
            listLayout.addView(t)
            return
        }
        results.sortedByDescending { it.level }.forEach { r ->
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 10, 0, 10)
                layoutParams = lp
                setPadding(12, 12, 12, 12)
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
                elevation = 4f
            }
            val ssidView = TextView(this)
            ssidView.text = "SSID: ${r.SSID}"
            ssidView.textSize = 16f
            container.addView(ssidView)

            val converted = convertSsid(r.SSID)
            val convView = TextView(this)
            convView.text = "القيمة: $converted"
            convView.textSize = 14f
            container.addView(convView)

            val copyBtn = Button(this)
            copyBtn.text = "نسخ"
            copyBtn.setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("wlan", converted))
                Toast.makeText(this, getString(R.string.copied, converted), Toast.LENGTH_SHORT).show()
            }
            container.addView(copyBtn)
            listLayout.addView(container)
        }
    }

    private fun convertSsid(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("fh_")) s = s.substring(3)
        val mapped = s.map { ch ->
            val lower = ch.lowercaseChar()
            map[lower] ?: ch
        }.joinToString("")
        return "wlan$mapped"
    }
}
