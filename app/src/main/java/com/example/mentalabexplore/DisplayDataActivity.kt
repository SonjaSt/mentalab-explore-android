package com.example.mentalabexplore

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DisplayDataActivity : AppCompatActivity() {

    lateinit var tabArray: Array<String>
    var menu: Menu? = null
    lateinit var mainHandler: Handler

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

        instance = this

        setContentView(R.layout.activity_display_data)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_baseline_menu_24)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
            this.overridePendingTransition(0, 0)
        }
        supportActionBar?.setDisplayShowTitleEnabled(false);

        val viewPager = findViewById<ViewPager2>(R.id.pager)
        val fragmentList = arrayListOf(
            ExgDataFragment.newInstance(),
            SensorDataFragment.newInstance(),
            OtherDataFragment.newInstance()
        )
        viewPager.adapter = DataPagerAdapter(this, fragmentList)

        tabArray = arrayOf (
            getString(R.string.exg_tab_text),
            getString(R.string.sensor_tab_text),
            getString(R.string.other_tab_text)
        )

        TabLayoutMediator(findViewById<TabLayout>(R.id.tabLayout), viewPager) { tab, position ->
            tab.text = tabArray[position]
        }.attach()

        var popupOverlay = findViewById<FrameLayout>(R.id.settings_overlay)
        var markerOverlay = findViewById<View>(R.id.marker_settings)
        var markerButton = findViewById<ImageButton>(R.id.marker_button)
        markerButton.setOnLongClickListener {
            popupOverlay.visibility = if(popupOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            markerOverlay.visibility = if(markerOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            true
        }

        mainHandler = Handler(Looper.getMainLooper())
    }



    override fun onBackPressed() {
        val caller = intent.getIntExtra("from", 0)
        val intent = when {
            caller == 1 -> Intent(this, ConnectBluetoothActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        Model.clearAllData()
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateModel)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateModel)
    }

    fun record(view: android.view.View) {}
    fun pushToLSL(view: android.view.View) {}
    fun setMarker(view: android.view.View) {
        //if(Model.isConnected) Model.updateDataCustomTimestamp()
        if(Model.isConnected) Model.setMarker()
    }
    fun setFilters(view: android.view.View) {}
    fun changeVisualizationSettings(view: android.view.View) {}

    companion object {
        lateinit var instance: DisplayDataActivity
        fun getActivity():DisplayDataActivity {
            return instance
        }
    }

}