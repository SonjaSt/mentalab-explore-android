package com.example.mentalabexplore

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun displayData(view: View) {
        val intent = Intent(this, DisplayDataActivity::class.java)
        startActivity(intent)
        finish()
    }

    // NOTE: This shows a dialog to enable discovery of nearby devices only twice (I think?) and I can't find out how to fix this
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("BLUETOOTH_CONNECT Permission: ", "Granted")
            } else {
                val t = Toast.makeText(this, "Please enable discovery of nearby devices in app permissions.", Toast.LENGTH_SHORT)
                t.show()
                Log.i("BLUETOOTH_CONNECT Permission: ", "Denied")
            }
        }

    fun findDevices(view: View) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED -> {
                val intent = Intent(this, ConnectBluetoothActivity::class.java)
                startActivity(intent)
                Log.d("MainActivity", "After finish()")
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }
        }
    }
}