package com.example.mentalabexplore

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import java.lang.Exception

class Settings : AppCompatActivity() {
    lateinit var mainHandler: Handler
    var activeChannels: MutableList<Switch> = mutableListOf<Switch>()
    var activeModules: MutableList<Switch> = mutableListOf<Switch>()

    lateinit var connectedName: TextView
    lateinit var temperature: TextView
    lateinit var battery: TextView

    var menu: Menu? = null

    val updateModel = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                connectedName.text = Model.connectedTo
                temperature.text = Model.getTemperatureString()
                battery.text = Model.getBatteryString()
            }
            Model.updateData()
            mainHandler.postDelayed(this, Model.refreshRate)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_twotone_show_chart_24)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            NavUtils.navigateUpFromSameTask(this)
            this.overridePendingTransition(0, 0)
            finish()
        }
        supportActionBar?.setDisplayShowTitleEnabled(false);

        connectedName = findViewById<TextView>(R.id.connected_name)
        temperature = findViewById<TextView>(R.id.temperature)
        battery = findViewById<TextView>(R.id.battery)

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
        Model.clearAllData()
        DisplayDataActivity.instance.finish()
        val intent = Intent(this, ConnectBluetoothActivity::class.java).putExtra("from", 2)
        startActivity(intent)
        finish()
    }

    fun applySettings(view: View) {
        // So, calling the setEnabled function from the API has some odd behaviour that I can't really explain or understand
        // For example, switching off the ExG module actually switches off the Orientation module
        // For this reason, the button is turned off (basically)
        return
        var activeChannelsMap = mapOf("CHANNEL_0" to activeChannels[0].isChecked!!,
            "CHANNEL_1" to activeChannels[1].isChecked!!,
            "CHANNEL_2" to activeChannels[2].isChecked!!,
            "CHANNEL_3" to activeChannels[3].isChecked!!,
            "CHANNEL_4" to activeChannels[4].isChecked!!,
            "CHANNEL_5" to activeChannels[5].isChecked!!,
            "CHANNEL_6" to activeChannels[6].isChecked!!,
            "CHANNEL_7" to activeChannels[7].isChecked!!)
        var activeModulesMap = mapOf("ModuleExg" to activeModules[0].isChecked!!,
            "ModuleOrn" to activeModules[1].isChecked!!)

        val res = Model.setEnabledChannelsAndModules(activeChannelsMap, activeModulesMap)
        if(!res) {
            var t = Toast.makeText(this, "Could not apply changes - are you trying to enable channels that are not available?", Toast.LENGTH_SHORT)
            t.show()
            //Model.getDataFromDevice()
            setChannelSwitchStates()
            setModuleSwitchStates()
            return
        }
        // TODO: apply settings
        NavUtils.navigateUpFromSameTask(this)
        this.overridePendingTransition(0, 0)
        finish()
    }

    fun disconnectDevice(view: android.view.View) {
        val b = findViewById<Button>(R.id.disconnect_button)
        b.background = getDrawable(R.drawable.rounded_corners_dark_gray)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            Model.clearAllData()
            b.background = getDrawable(R.drawable.rounded_corners_gray)
            connectedName.text = ""
            battery.text = ""
            temperature.text = ""
            val t = Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT)
            t.show()
        }, 500)
    }

    fun deleteMemory(view: android.view.View) {
        if(!Model.isConnected) return
        val dialog: AlertDialog? = this?.let {
            val builder = AlertDialog.Builder(it)
            builder?.setMessage("Are you sure you want to format the device memory?\nThis will delete all data on the device and disconnect it.")
            builder.apply {
                setPositiveButton("Yes", DialogInterface.OnClickListener {
                    dialog, id -> callDelete()
                })
                setNegativeButton("No", DialogInterface.OnClickListener {
                    dialog, id -> {
                }
                })
            }
            builder.create()
        }
        dialog?.show()
    }

    fun callDelete(){
        try {
            Model.formatDeviceMemory()
            Model.clearAllData()
            connectedName.text = ""
            battery.text = ""
            temperature.text = ""
        }
        catch(e: Exception) {
            var t = Toast.makeText(this, "Could not format device memory!", Toast.LENGTH_SHORT)
            t.show()
        }
    }
}