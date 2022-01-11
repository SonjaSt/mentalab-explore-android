package com.example.mentalabexplore

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
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
        color = 0xff000000.toInt() // this is fully opaque black
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
                //Log.d("CURRENT STREAMTAG", streamTag)
                var startY = yAxisY2-(start!!+Model.getMax(streamTag).absoluteValue)/(2.0f*Model.getMax(streamTag).absoluteValue) * spacevertical
                var stopY = yAxisY2-(end!!+Model.getMax(streamTag).absoluteValue)/(2.0f*Model.getMax(streamTag).absoluteValue) * spacevertical
                //Log.d("ONDRAW", "Start Y: $startY")
                //Log.d("ONDRAW", "Stop Y: $stopY")
                //Log.d("ONDRAW", "Y Axis Start Y: $yAxisY1")
                //Log.d("ONDRAW", "Y Axis Stop Y: $yAxisY2")
                //Log.d("ONDRAW", "DATAPOINT: $end")
                canvas?.drawLine((i-1)*spacehorizontal/datapoints.size + paddingHorizontal, startY, i*spacehorizontal/datapoints.size + paddingHorizontal, stopY, paint)
                //canvas?.drawLine(0.0f, 0.0f, this.measuredWidth.toFloat(), this.measuredHeight.toFloat(), paint)
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

class SensorChart @JvmOverloads constructor(
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
        color = 0xff000000.toInt() // this is fully opaque red
        strokeWidth = 2.0f
    }

    private val paint_red = Paint().apply {
        color = 0xffff0000.toInt() // this is fully opaque red
        strokeWidth = 2.0f
    }

    private val paint_green = Paint().apply {
        color = 0xff00ff00.toInt() // this is fully opaque red
        strokeWidth = 2.0f
    }

    private val paint_blue = Paint().apply {
        color = 0xff0000ff.toInt() // this is fully opaque red
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

        val tags: List<String> = listOf("${streamTag}_X", "${streamTag}_Y", "${streamTag}_Z")

        for(tag in tags) {
            var datapoints = Model.getData(tag)
            var start: Float? = null
            var end = 0.0f
            for ((i, datapoint) in datapoints.withIndex()) {

                end = datapoint

                start?.let {
                    //Log.d("CURRENT STREAMTAG", streamTag)
                    var startY =
                        yAxisY2 - (start!! + Model.getMax(tag).absoluteValue) / (2.0f * Model.getMax(
                            tag
                        ).absoluteValue) * spacevertical
                    var stopY =
                        yAxisY2 - (end!! + Model.getMax(tag).absoluteValue) / (2.0f * Model.getMax(
                            tag
                        ).absoluteValue) * spacevertical
                    //Log.d("ONDRAW", "Start Y: $startY")
                    //Log.d("ONDRAW", "Stop Y: $stopY")
                    //Log.d("ONDRAW", "Y Axis Start Y: $yAxisY1")
                    //Log.d("ONDRAW", "Y Axis Stop Y: $yAxisY2")
                    //Log.d("ONDRAW", "DATAPOINT: $end")
                    var p = paint
                    when(tag.last()){
                        'X' -> p = paint_red
                        'Y' -> p = paint_green
                        'Z' -> p = paint_blue

                    }
                    canvas?.drawLine(
                        (i - 1) * spacehorizontal / datapoints.size + paddingHorizontal,
                        startY,
                        i * spacehorizontal / datapoints.size + paddingHorizontal,
                        stopY,
                        p
                    )
                    //canvas?.drawLine(0.0f, 0.0f, this.measuredWidth.toFloat(), this.measuredHeight.toFloat(), paint)
                }
                if(tag.last() == 'X') {
                    if (i % xStep == 0 && i != 0) {
                        canvas?.drawLine(
                            (i - 1) * spacehorizontal / datapoints.size + paddingHorizontal,
                            xAxisY - 5.0f,
                            (i - 1) * spacehorizontal / datapoints.size + paddingHorizontal,
                            xAxisY + 5.0f,
                            paint
                        )
                    }
                }

                start = datapoint
            }
            if(tag.last() == 'X') {
                val avgY = (yAxisY2 - yAxisY1) / 2
                //val realZero = Model.getAverage()/1000 * spacevertical + paddingVertical
                canvas?.drawLine(yAxisX - 5.0f, avgY, yAxisX + 5.0f, avgY, paint)
                //canvas?.drawLine(yAxisX-5.0f, realZero, yAxisX+5.0f, realZero, paint)
            }
        }
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

    var channels: MutableList<LineChart> = mutableListOf()


    val updateChartDelayed = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                var activeChannels = Model.getActiveChannels()
                if(activeChannels != null) {
                    for (i in 1..8) {
                        if (activeChannels.contains("Channel_${i}")) {
                            //Log.d("Active channels", "Channel_${i-1}")
                            if (channels[i - 1].isGone) channels[i - 1].visibility = View.VISIBLE
                            Model.insertDataFromDevice("Channel_${i}")
                            channels[i-1].invalidate()
                        }
                        else {
                            if (channels[i - 1].isVisible) channels[i - 1].visibility = View.GONE
                        }
                    }
                }
                else {
                    for(e in channels) {
                        if(e.isVisible) e.visibility = View.GONE
                    }
                }
            }
            else {
                for(e in channels) {
                    if(e.isVisible) e.visibility = View.GONE
                }
            }
            //mainHandler.post(this)
            mainHandler.postDelayed(this, 50)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val baseView = inflater.inflate(R.layout.exg_fragment, container, false)

        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_1))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_2))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_3))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_4))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_5))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_6))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_7))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_8))

        for(i in 1..8){
            channels[i-1].streamTag = "Channel_${i}"
            //Log.d("CHANNELS", "${channels[i-1].streamTag}, ID ${i}")
        }

        for(i in channels)
        {
            //Log.d("Channel: ", i.streamTag)
        }

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

    lateinit var gyroscope: SensorChart
    lateinit var accelerometer: SensorChart
    lateinit var magnetometer: SensorChart
    var delay: Long = 0

    lateinit var mainHandler: Handler

    val updateChartDelayed = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                if(Model.isGyroscopeActive()) {
                    Model.insertDataFromDevice("Gyro_X")
                    Model.insertDataFromDevice("Gyro_Y")
                    Model.insertDataFromDevice("Gyro_Z")
                    if(!gyroscope.isVisible) gyroscope.visibility = View.VISIBLE
                    gyroscope.invalidate()
                }
                else {
                    if(gyroscope.isVisible) gyroscope.visibility = View.GONE
                }

                if(Model.isAccelerometerActive()) {
                    Model.insertDataFromDevice("Acc_X")
                    Model.insertDataFromDevice("Acc_Y")
                    Model.insertDataFromDevice("Acc_Z")
                    if(!accelerometer.isVisible) accelerometer.visibility = View.VISIBLE
                    accelerometer.invalidate()
                }
                else {
                    if(accelerometer.isVisible) accelerometer.visibility = View.GONE
                }

                if(Model.isAccelerometerActive()) {
                    Model.insertDataFromDevice("Mag_X")
                    Model.insertDataFromDevice("Mag_Y")
                    Model.insertDataFromDevice("Mag_Z")
                    if(!magnetometer.isVisible) magnetometer.visibility = View.VISIBLE
                    magnetometer.invalidate()
                }
                else {
                    if(magnetometer.isVisible) magnetometer.visibility = View.GONE
                }
            }
            else {
                if(gyroscope.isVisible) gyroscope.visibility = View.GONE
                if(accelerometer.isVisible) accelerometer.visibility = View.GONE
                if(magnetometer.isVisible) magnetometer.visibility = View.GONE
            }

            //mainHandler.post(this)
            mainHandler.postDelayed(this, 50)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val baseview = inflater.inflate(R.layout.sensors_fragment, container, false)

        gyroscope = baseview.findViewById<SensorChart>(R.id.gyroscope)
        accelerometer = baseview.findViewById<SensorChart>(R.id.accelerometer)
        magnetometer = baseview.findViewById<SensorChart>(R.id.magnetometer)

        gyroscope.streamTag = "Gyro"
        accelerometer.streamTag = "Acc"
        magnetometer.streamTag = "Mag"

        mainHandler = Handler(Looper.getMainLooper())

        return baseview
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