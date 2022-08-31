package com.example.mentalabexplore

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
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
import com.mentalab.utils.ConfigSwitch
import com.mentalab.utils.constants.ConfigProtocol
import com.mentalab.utils.constants.SamplingRate
import java.lang.Exception
import java.util.concurrent.TimeUnit

class Settings : AppCompatActivity() {
    lateinit var mainHandler: Handler
    var activeChannels: MutableList<Switch> = mutableListOf()
    var activeModules: MutableList<Switch> = mutableListOf()

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
        if (Model.isConnected) {
            if (Model.device!!.channelCount.asInt == 8) {
                activeChannels.add(findViewById(R.id.ch5_switch))
                activeChannels.add(findViewById(R.id.ch6_switch))
                activeChannels.add(findViewById(R.id.ch7_switch))
                activeChannels.add(findViewById(R.id.ch8_switch))
            } else {
                findViewById<Switch>(R.id.ch5_switch).visibility = View.GONE
                findViewById<Switch>(R.id.ch6_switch).visibility = View.GONE
                findViewById<Switch>(R.id.ch7_switch).visibility = View.GONE
                findViewById<Switch>(R.id.ch8_switch).visibility = View.GONE
            }
        }

        setChannelSwitchStates()

        activeModules.add(findViewById(R.id.exg_switch))
        activeModules.add(findViewById(R.id.sensors_switch))

        setModuleSwitchStates()

        mainHandler = Handler(Looper.getMainLooper())
    }

    fun setChannelSwitchStates() {
        val channels = Model.getActiveChannels()
        for(i in 0..3) {
            activeChannels[i].isChecked =
                !(channels == null || channels[i])
        }
        if(Model.device != null){
            for(i in 4..7) {
                activeChannels[i].isChecked =
                    !(channels == null || channels[i])
            }
        }
    }

    /*
    fun setModuleSwitchStates() {
        val keys = Model.getDeviceKeys()
        val channel = Model.getActiveChannels()
        activeModules[0].isChecked = !(channel == null || channel.isEmpty())
        activeModules[1].isChecked = !(keys == null || !keys.contains("Acc_X"))
        //activeModules[2].isChecked = !(keys == null || !keys.contains("Temperature "))
    }
    */

    fun setModuleSwitchStates() {
        activeModules[0].isChecked = true
        activeModules[1].isChecked = true
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
        var r = Model.device!!.setChannel(ConfigSwitch(ConfigProtocol.CHANNEL_0, false))
        var w = Model.device!!.setSamplingRate(SamplingRate.SR_500)
        Model.device!!.channelCount.asInt
        if(r.get(2000, TimeUnit.MILLISECONDS)) {
            // Do something
        }
        else {
            Toast.makeText(this, "Failed to set channel", Toast.LENGTH_SHORT)
        }

        var l = mutableListOf<Boolean>()
        for(switch in activeChannels) {
            l.add(switch.isChecked)
        }
        Model.setChannels(l)
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