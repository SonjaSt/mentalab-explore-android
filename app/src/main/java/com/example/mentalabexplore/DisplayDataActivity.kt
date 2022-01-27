package com.example.mentalabexplore

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
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

        mainHandler = Handler(Looper.getMainLooper())
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
}