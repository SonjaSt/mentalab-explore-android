package com.example.mentalabexplore

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DisplayDataActivity : AppCompatActivity() {

    val tabArray = arrayOf (
        "ExG",
        "Sensors",
        "Other"
    )

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

        TabLayoutMediator(findViewById<TabLayout>(R.id.tabLayout), viewPager) { tab, position ->
            tab.text = tabArray[position]
        }.attach()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}