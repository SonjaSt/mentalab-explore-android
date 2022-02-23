package com.example.mentalabexplore

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat

class ConnectBluetoothActivity : AppCompatActivity() {
    lateinit var selectedDevice: String
    var activeButton: TextView? = null
    lateinit var devices: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_bluetooth)

        devices = findViewById<LinearLayout>(R.id.bluetooth_devices)

        scan()
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun connect(view: View) {
        // TODO do all of this in another thread / in the background
        // Currently, everything is blocked during connection
        if(Model.connectDevice(selectedDevice)) {
            val intent = Intent(this, DisplayDataActivity::class.java).putExtra("from", 1)
            startActivity(intent)
            finish()
        }
        else {
            val t = Toast.makeText(this, "Could not connect to selected device", Toast.LENGTH_SHORT)
            t.show()
        }
    }

    fun rescan(view: View) {
        devices.removeAllViews()
        // Might be a good idea to add a tiny wait period here so the user doesn't think the app is frozen if nothing new is found
        scan()
    }

    fun scan() {
        val deviceSet = Model.scanDevices(applicationContext)


        for (device in deviceSet) {
            val deviceView = TextView(this)
            deviceView.text = device
            deviceView.setPadding(0, 30, 0, 30)
            deviceView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            deviceView.background = getDrawable(R.drawable.rounded_corners_gray)
            deviceView.setOnClickListener {
                findViewById<Button>(R.id.connect_button).visibility = View.VISIBLE
                activeButton?.background = getDrawable(R.drawable.rounded_corners_gray)
                selectedDevice = device
                activeButton = deviceView
                deviceView.background = getDrawable(R.drawable.rounded_corners_dark_gray)
            }
            devices.addView(deviceView)

            val space = Space(this)
            space.setMinimumHeight(40)
            devices.addView(space)
        }
    }
}