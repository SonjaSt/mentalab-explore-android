package com.example.mentalabexplore

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DisplayDataActivity : AppCompatActivity() {

    lateinit var tabArray: Array<String>
    var menu: Menu? = null
    lateinit var mainHandler: Handler

    lateinit var popupOverlay: FrameLayout
    lateinit var markerOverlay: LinearLayout
    lateinit var visualisationOverlay: LinearLayout

    lateinit var markerButton: ImageButton
    lateinit var visualisationButton: ImageButton

    lateinit var connectedName: TextView
    lateinit var temperature: TextView
    lateinit var battery: TextView

    var currentPopup: String = ""

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

        instance = this

        setContentView(R.layout.activity_display_data)

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_baseline_settings_24)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
            this.overridePendingTransition(0, 0)
        }
        supportActionBar?.setDisplayShowTitleEnabled(false);

        connectedName = findViewById<TextView>(R.id.connected_name)
        temperature = findViewById<TextView>(R.id.temperature)
        battery = findViewById<TextView>(R.id.battery)

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

        connectButtons()

        popupOverlay = findViewById<FrameLayout>(R.id.settings_overlay)
        markerOverlay = findViewById<LinearLayout>(R.id.marker_settings)
        visualisationOverlay = findViewById<LinearLayout>(R.id.visualisation_settings)


        val yAxisSpinner: Spinner = findViewById(R.id.y_axis_spinner)
        ArrayAdapter.createFromResource(this,
            R.array.y_axis_choices,
            android.R.layout.simple_spinner_item).also {
                adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            yAxisSpinner.adapter = adapter
        }

        yAxisSpinner.setSelection(Model.rangeToSelection())

        yAxisSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Model.changeRange(p0?.getItemAtPosition(p2).toString())
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        mainHandler = Handler(Looper.getMainLooper())
    }

    //TODO fix the listeners so other buttons are blocked when a popup is open
    private fun connectButtons() {
        markerButton = findViewById<ImageButton>(R.id.marker_button)
        markerButton.setOnLongClickListener {
            popupOverlay.visibility = if(popupOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            markerOverlay.visibility = if(markerOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            true
        }

        visualisationButton = findViewById<ImageButton>(R.id.visualisation_button)
        visualisationButton.setOnLongClickListener {
            popupOverlay.visibility = if(popupOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            visualisationOverlay.visibility = if(visualisationOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            true
        }
    }


    override fun onBackPressed() {
        // to know where we came from initially
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

    //@RequiresApi(Build.VERSION_CODES.Q)
    fun record(view: android.view.View) {
        //Model.recordData(this)
    }
    fun pushToLSL(view: android.view.View) {
        //Model.pushDataToLSL()
    }
    fun setMarker(view: android.view.View) {
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