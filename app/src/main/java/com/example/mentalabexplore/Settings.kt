package com.example.mentalabexplore

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils

class Settings : AppCompatActivity() {
    lateinit var mainHandler: Handler
    var activeChannels: MutableList<Switch> = mutableListOf<Switch>()
    var activeModules: MutableList<Switch> = mutableListOf<Switch>()

    var menu: Menu? = null

    val updateModel = object : Runnable {
        override fun run() {
            menu?.let {
                if(!Model.isConnected) {
                    return@let
                }
                var connectedDevice = menu!!.findItem(R.id.action_connectedDevice)
                var temperature = menu!!.findItem(R.id.action_temperature)
                var battery = menu!!.findItem(R.id.action_battery)
                connectedDevice.setTitle(Model.connectedTo)
                temperature.setTitle(Model.getTemperatureString())
                battery.setTitle(Model.getBatteryString())
            }
            Model.updateData()
            mainHandler.postDelayed(this, Model.refreshRate)
        }
    }

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

        activeChannels.add(findViewById(R.id.ch1_switch))
        activeChannels.add(findViewById(R.id.ch2_switch))
        activeChannels.add(findViewById(R.id.ch3_switch))
        activeChannels.add(findViewById(R.id.ch4_switch))
        activeChannels.add(findViewById(R.id.ch5_switch))
        activeChannels.add(findViewById(R.id.ch6_switch))
        activeChannels.add(findViewById(R.id.ch7_switch))
        activeChannels.add(findViewById(R.id.ch8_switch))

        setChannelSwitchStates()

        activeModules.add(findViewById(R.id.exg_switch))
        activeModules.add(findViewById(R.id.sensors_switch))

        setModuleSwitchStates()

        mainHandler = Handler(Looper.getMainLooper())
    }

    fun setChannelSwitchStates() {
        val channels = Model.getActiveChannels()
        for(i in 0..7) {
            activeChannels[i].isChecked =
                !(channels == null || !channels.contains("Channel_${i+1}"))
        }
    }

    fun setModuleSwitchStates() {
        val keys = Model.getDeviceKeys()
        val channel = Model.getActiveChannels()
        activeModules[0].isChecked =  !(channel == null || channel.isEmpty())
        activeModules[1].isChecked = !(keys == null || !keys.contains("Acc_X"))
        //activeModules[2].isChecked = !(keys == null || !keys.contains("Temperature "))
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateModel)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateModel)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        this.menu = menu
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