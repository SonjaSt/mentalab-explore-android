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
import kotlin.math.absoluteValue
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
    var streamTag: String = ""

    var paddingHorizontal = 20.0f
    var paddingVertical = 20.0f

    var yAxisX = paddingHorizontal
    var yAxisY1 = paddingVertical
    var yAxisY2 = yAxisY1

    var xAxisX1 = yAxisX
    var xAxisX2 = xAxisX1
    var xAxisY = yAxisY2

    var xStep = 10

    var spacehorizontal = 0.0f
    var spacevertical = 0.0f

    private val paint = Paint().apply {
        color = 0xff000000.toInt()
        strokeWidth = 2.0f
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        yAxisY2 = this.measuredHeight.toFloat() - paddingVertical
        xAxisX2 = this.measuredWidth.toFloat() - paddingHorizontal
        xAxisY = yAxisY2
        spacehorizontal = this.measuredWidth - 2*paddingHorizontal
        spacevertical = this.measuredHeight - 4*paddingVertical
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //Log.d("ONDRAW", "streamtag: $streamTag")
        canvas?.drawLine(yAxisX, yAxisY1, yAxisX, yAxisY2, paint)
        canvas?.drawLine(xAxisX1, xAxisY, xAxisX2, xAxisY, paint)

        var datapoints = Model.getData(streamTag)
        var start: Float? = null
        var end = 0.0f
        for((i, datapoint) in datapoints.withIndex()) {

            end = datapoint
            start?.let {
                var startY = yAxisY2-(start!!+Model.getMax(streamTag).absoluteValue)/(2.0f*Model.getMax(streamTag).absoluteValue) * spacevertical
                var stopY = yAxisY2-(end!!+Model.getMax(streamTag).absoluteValue)/(2.0f*Model.getMax(streamTag).absoluteValue) * spacevertical
                //Log.d("ONDRAW", "Start Y: $startY")
                //Log.d("ONDRAW", "Stop Y: $stopY")
                //Log.d("ONDRAW", "Y Axis Start Y: $yAxisY1")
                //Log.d("ONDRAW", "Y Axis Stop Y: $yAxisY2")
                //Log.d("ONDRAW", "DATAPOINT: $end")
                canvas?.drawLine((i-1)*spacehorizontal/datapoints.size + paddingHorizontal, startY, i*spacehorizontal/datapoints.size + paddingHorizontal, stopY, paint)
            }

            if (i % xStep == 0 && i != 0) {
                canvas?.drawLine((i-1)*spacehorizontal/datapoints.size + paddingHorizontal, xAxisY-5.0f, (i-1)*spacehorizontal/datapoints.size + paddingHorizontal, xAxisY+5.0f, paint)
            }

            start = datapoint
        }

        val avgY = (yAxisY2-yAxisY1)/2
        //val realZero = Model.getAverage()/1000 * spacevertical + paddingVertical
        canvas?.drawLine(yAxisX-5.0f, avgY, yAxisX+5.0f, avgY, paint)
        //canvas?.drawLine(yAxisX-5.0f, realZero, yAxisX+5.0f, realZero, paint)
    }
}

class LineChart2 : CardView {
    var paint: Paint = Paint().apply {
        color = 0xffff0000.toInt()
        strokeWidth = 2.0f
    }

    var startx = 5.0f
    var starty = 5.0f
    var endx = startx
    var endy = 0.0f

    var paddingHorizontal = 2*startx
    var paddingVertical = 2*starty


    constructor(context: Context, ) : super(context) {
        endy = this.measuredHeight.toFloat() - 5.0f
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawLine(startx, starty, endx, endy, paint)

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
    lateinit var chart1 : LineChart
    lateinit var chart2 : LineChart

    val updateChartDelayed = object : Runnable {
        override fun run() {
            Model.insertDataFromDevice("Gyro_X")
            Model.insertDataFromDevice("Gyro_Z")
            chart1.invalidate()
            chart2.invalidate()
            //mainHandler.post(this)
            mainHandler.postDelayed(this, 100)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val baseView = inflater.inflate(R.layout.exg_fragment, container, false)
        chart1 = baseView.findViewById<LineChart>(R.id.card_view)
        chart1.streamTag = "Gyro_X"
        chart2 = baseView.findViewById<LineChart>(R.id.card_view2)
        chart2.streamTag = "Gyro_Z"

        mainHandler = Handler(Looper.getMainLooper())

        return baseView
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(updateChartDelayed)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(updateChartDelayed)
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