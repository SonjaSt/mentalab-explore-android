package com.example.mentalabexplore

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            NavUtils.navigateUpFromSameTask(this)
            this.overridePendingTransition(0, 0)
            finish()
        }
        supportActionBar?.setDisplayShowTitleEnabled(false);

        val spinner: Spinner = findViewById(R.id.spinner2)
        ArrayAdapter.createFromResource(this,
            R.array.samples_rates,
            android.R.layout.simple_spinner_item).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
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

    fun applySettings(view: View) {
        // TODO: apply settings
        NavUtils.navigateUpFromSameTask(this)
        this.overridePendingTransition(0, 0)
        finish()
    }
}