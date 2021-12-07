package com.example.mentalabexplore

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

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

    fun findDevices(view: View) {
        try{
            Model.scanDevices(applicationContext)
            Log.d("MainActivity", "Successfully scanned for devices.")
        } catch(e: Exception) {
            Log.d("MainActivity", "Encountered error while scanning for devices.")
            e.printStackTrace()
        }
    }
}