package com.example.mentalabexplore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.random.Random

class DataPagerAdapter(fragmentActivity: FragmentActivity, val fragments:ArrayList<Fragment>) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = fragments.size   // Note: This should always be 3

    override fun createFragment(position: Int): Fragment = fragments[position]
}

class LineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    private val paint = Paint().apply {
        color = 0xffff0000.toInt()
        strokeWidth = 5.0f
    }

    fun generateRandomDatapoints() {

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var array : FloatArray
        var randomY1 = Random.nextInt(this.measuredHeight).toFloat()
        var randomY2 = Random.nextInt(this.measuredHeight).toFloat()
        val numSteps = 10
        for(i in (0..numSteps)) {
            canvas?.drawLine(i * (this.measuredWidth/numSteps).toFloat(), randomY1, (i+1) * (this.measuredWidth/numSteps).toFloat(), randomY2, paint)
            randomY1 = randomY2
            randomY2 = Random.nextInt(this.measuredHeight).toFloat()
        }
    }
}


class ExgDataFragment : Fragment() {

    lateinit var mainHandler : Handler
    lateinit var chart1 : CardView
    lateinit var chart2 : CardView

    val updateChart = object : Runnable {
        override fun run() {
            chart1.invalidate()
            chart2.invalidate()
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val baseView = inflater.inflate(R.layout.exg_fragment, container, false)
        chart1 = baseView.findViewById<LineChart>(R.id.card_view)
        chart2 = baseView.findViewById<LineChart>(R.id.card_view2)

        mainHandler = Handler(Looper.getMainLooper())

        return baseView
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateChart)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateChart)
    }

    companion object{
        fun newInstance() = ExgDataFragment()
    }

}

class SensorDataFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sensors_fragment, container, false)
    }

    companion object{
        fun newInstance() = SensorDataFragment()
    }
}

class OtherDataFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.other_fragment, container, false)
    }

    companion object{
        fun newInstance() = OtherDataFragment()
    }
}