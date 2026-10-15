package com.mrt.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var clockText: TextView
    private lateinit var gpsStatusText: TextView
    private lateinit var speedText: TextView
    private lateinit var gpsButton: Button

    private var locationManager: LocationManager? = null
    private var isGpsActive = false

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speedKmh = location.speed * 3.6f
            runOnUiThread {
                speedText.text = String.format(Locale.US, "%.0f", speedKmh)
                gpsStatusText.text = "● GPS LIVE"
            }
        }
        override fun onProviderEnabled(provider: String) {
            runOnUiThread { gpsStatusText.text = "● GPS ENABLED" }
        }
        override fun onProviderDisabled(provider: String) {
            runOnUiThread { gpsStatusText.text = "○ GPS DISABLED" }
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        gpsStatusText = findViewById(R.id.gpsStatusText)
        speedText = findViewById(R.id.speedText)
        gpsButton = findViewById(R.id.gpsButton)

        gpsButton.setOnClickListener {
            if (!isGpsActive) startGps() else stopGps()
        }

        handler.post(clockRunnable)
        Toast.makeText(this, "MRT1 Ready", Toast.LENGTH_SHORT).show()
    }

    private fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        clockText.text = sdf.format(Date())
    }

    @SuppressLint("MissingPermission")
    private fun startGps() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
            return
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000L, 0f, locationListener)
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                1000L, 0f, locationListener)
            isGpsActive = true
            gpsButton.text = "STOP GPS"
            gpsStatusText.text = "● GPS STARTING..."
        } catch (e: Exception) {
            gpsStatusText.text = "✕ GPS ERROR"
        }
    }

    private fun stopGps() {
        locationManager?.removeUpdates(locationListener)
        isGpsActive = false
        gpsButton.text = "START GPS"
        gpsStatusText.text = "○ GPS STOPPED"
        speedText.text = "--"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGps()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
        if (isGpsActive) {
            try { locationManager?.removeUpdates(locationListener) } catch (e: Exception) {}
        }
    }
}
