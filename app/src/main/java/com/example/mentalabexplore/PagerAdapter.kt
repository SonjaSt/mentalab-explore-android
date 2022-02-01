package com.example.mentalabexplore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlin.math.absoluteValue

class DataPagerAdapter(fragmentActivity: FragmentActivity, val fragments:ArrayList<Fragment>) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = fragments.size   // Note: This should always be 3 (3 tabs)

    override fun createFragment(position: Int): Fragment = fragments[position]
}

// TODO LineChart and Sensorchart should probably both be children of a chart class as they
//  share a substantial amount of variables and (possibly?) code
class LineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    // streamTag determines what data is display by the chart, i.e. "Channel_1", "Gyro_Y" etc.
    var streamTag: String = ""

    var paddingHorizontal = 40.0f
    var paddingVertical = 60.0f

    // Coordinates of the y- and x-axis
    var yAxisX = paddingHorizontal
    var yAxisY1 = paddingVertical
    var yAxisY2 = yAxisY1 // Can only be set properly on layout change

    var xAxisX1 = yAxisX
    var xAxisX2 = xAxisX1
    var xAxisY = yAxisY2

    var spacehorizontal = 0.0f
    var spacevertical = 0.0f

    // transform holds the transformation matrix applied to the incoming datapoints
    var transform: Matrix = Matrix()


    private val paint = Paint().apply {
        color = 0xff000000.toInt() // this is fully opaque black
        strokeWidth = 2.0f
    }
    private val paint_baseline = Paint().apply {
        color = 0xffaaaaaa.toInt()
        strokeWidth = 2.0f
    }
    private val paint_text = Paint().apply {
        color = 0xffaaaaaa.toInt()
        strokeWidth = 2.0f
        textAlign = Paint.Align.RIGHT
        textSize = 30.0f
    }
    private val paint_marker = Paint().apply {
        color = 0xffaa0000.toInt()
        strokeWidth = 2.0f
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        yAxisY2 = this.measuredHeight.toFloat() - paddingVertical
        xAxisX2 = this.measuredWidth.toFloat()
        xAxisY = yAxisY2

        // Available height and width for the chart
        spacehorizontal = this.measuredWidth - paddingHorizontal // only pad the left side
        spacevertical = this.measuredHeight - 2*paddingVertical
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val avgY = (yAxisY2-yAxisY1)/2 + paddingVertical
        canvas?.drawLine(xAxisX1, avgY, xAxisX2, avgY, paint_baseline) // Horizontal baseline

        paint_text.textAlign = Paint.Align.LEFT
        canvas?.drawText("(+${Model.scaleToVolts()})", yAxisX+5.0f, yAxisY1+5.0f+paint_text.textSize, paint_text)
        canvas?.drawText("(-${Model.scaleToVolts()})", yAxisX+5.0f, yAxisY2-5.0f-paint_text.textSize, paint_text)

        canvas?.drawLine(yAxisX, yAxisY1, yAxisX, yAxisY2, paint) // y-Axis
        canvas?.drawLine(xAxisX1, xAxisY, xAxisX2, xAxisY, paint) // x-Axis

        //val mVals: FloatArray = floatArrayOf(this.measuredWidth.toFloat()/Model.maxElements, 0.0f, 0.0f, 0.0f, 1.0f/scale_y, (this.measuredHeight.toFloat()/2) - (Model.getAverage(streamTag)/scale_y), 0.0f, 0.0f, 1.0f)
        var xNum = Model.getData(streamTag).size
        if(xNum < 1) xNum = Model.maxElements

        // (width/max_elements)    0              padding_left
        // 0                       (-1/range_y)*height    (avg/range_y)*height + (height/2) + padding_top
        // 0                       0              1

        // -> y = (-1/range_y)*height*POINT + (avg/range_y)*height + (height/2) + padding_top
        // where range is the uV Range on the y-Axis, height is the available vertical space
        // and avg is the unscaled uV average of the channel
        // -> y is scaled by the available range given in terms of available height and inverted
        // (because the android coordinate system has a downward facing y-axis)
        // afterwards, y is shifted by the channel average (scaled in the same way) to a new "fake" 0
        // then, y is shifted further down to the middle of the available space (height/2 + padding_top)

        var mVals = floatArrayOf(
            (spacehorizontal)/xNum, 0.0f, paddingHorizontal,
            0.0f, -1.0f/Model.range_y*spacevertical, Model.getAverage(streamTag)/Model.range_y*spacevertical+(spacevertical/2.0f)+paddingVertical,
            0.0f, 0.0f, 1.0f)
        transform.setValues(mVals)

        //Make an array of points for the transformation [x1, y1, x2, y2, ..., x_n, y_n]
        var transPoints: FloatArray = FloatArray(Model.getData(streamTag).size*2)
        for((i, datapoint) in Model.getData(streamTag).withIndex()) {
            transPoints.set(i*2, i.toFloat())
            transPoints.set(i*2+1, datapoint)
        }

        transform.mapPoints(transPoints)

        var start_x = if (transPoints.size >= 2) transPoints[0] else 0.0f
        var start_y = if (transPoints.size >= 2) transPoints[1] else 0.0f
        var end_x = 0.0f
        var end_y = 0.0f

        // go through all available points and draw a line to it from the last point
        for(i in 0..(transPoints.size-1) step 2) {
            end_x = transPoints[i]
            end_y = transPoints[i+1]
            canvas?.drawLine(start_x, start_y, end_x, end_y, paint)
            Model.timestamps[i/2]?.let{
                // Draw the tick on the x-axis
                var x = (i / 2) * (spacehorizontal) / xNum + paddingHorizontal
                if(it != Model.markerTimestamp) {
                    canvas?.drawLine(x, xAxisY + 5.0f, x, xAxisY - 5.0f, paint_baseline)
                    paint_text.textAlign = Paint.Align.CENTER
                    canvas?.drawText(
                        Model.millisToMinutes(it),
                        x,
                        xAxisY + 10.0f + paint_text.textSize,
                        paint_text
                    ) // it = Model.timestamps[i/2]
                }
                else {
                    paint_marker.color = Model.markerColor.toInt()
                    canvas?.drawLine(x, 0.0f, x, xAxisY + 5.0f, paint_marker)
                    paint_text.textAlign = Paint.Align.CENTER
                    //canvas?.drawText(Model.millisToMinutes(it), x, xAxisY + 10.0f + paint_text.textSize, paint_text)
                }
            }
            start_x = transPoints[i]
            start_y = transPoints[i+1]
        }
    }
}

// A lot of this is very similar to the Linechart class
// TODO: Use a matrix to transform sensor data depending on sensor maxes
class SensorChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {
    var matrices: Array<Matrix> = arrayOf(Matrix(), Matrix(), Matrix())

    var streamTag: String = ""
    var ind = -1

    var paddingHorizontal = 40.0f
    var paddingVertical = 60.0f

    var yAxisX = paddingHorizontal
    var yAxisY1 = paddingVertical
    var yAxisY2 = yAxisY1

    var xAxisX1 = yAxisX
    var xAxisX2 = xAxisX1
    var xAxisY = yAxisY2

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

    private val paint_text = Paint().apply {
        color = 0xffaaaaaa.toInt()
        strokeWidth = 2.0f
        textAlign = Paint.Align.LEFT
        textSize = 30.0f
    }

    private val paint_baseline = Paint().apply {
        color = 0xffaaaaaa.toInt()
        strokeWidth = 2.0f
    }

    private val paint_marker = Paint().apply {
        color = 0xffaa0000.toInt()
        strokeWidth = 2.0f
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        yAxisY2 = this.measuredHeight.toFloat() - paddingVertical
        xAxisX2 = this.measuredWidth.toFloat()
        xAxisY = yAxisY2

        spacehorizontal = this.measuredWidth - paddingHorizontal // pad only on the left
        spacevertical = this.measuredHeight - 2*paddingVertical

        when(streamTag) {
            "Gyro" -> ind = 0
            "Acc" -> ind = 1
            "Mag" -> ind = 2
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //Log.d("ONDRAW", "streamtag: $streamTag")

        canvas?.drawLine(yAxisX, yAxisY1, yAxisX, yAxisY2, paint) // y-Axis
        canvas?.drawLine(xAxisX1, xAxisY, xAxisX2, xAxisY, paint) // x-Axis

        val tags: Array<String> = arrayOf("${streamTag}_X", "${streamTag}_Y", "${streamTag}_Z")

        for(tag in tags) {
            var datapoints = Model.getData(tag)

            var xNum = datapoints.size
            if(xNum < 1) xNum = Model.maxElements

            var mVals = floatArrayOf(
                spacehorizontal/xNum, 0.0f, paddingHorizontal,
                0.0f, -1.0f/Model.sensorMaxes[ind].absoluteValue * spacevertical, 0.0f,
                0.0f, 0.0f, 1.0f)
            matrices[ind].setValues(mVals)

            var start: Float? = null
            var end = 0.0f
            for ((i, datapoint) in datapoints.withIndex()) {

                end = datapoint

                start?.let {
                    //Log.d("CURRENT STREAMTAG", streamTag)
                    var startY =
                        yAxisY2 - (start!! + Model.sensorMaxes[ind].absoluteValue) / (2.0f * Model.sensorMaxes[ind].absoluteValue) * spacevertical
                    //startY = start!! * spacevertical/2.0f * Model.sensorMaxes[ind].absoluteValue + paddingVertical + spacevertical/2.0f
                    var stopY =
                        yAxisY2 - (end!! + Model.sensorMaxes[ind].absoluteValue) / (2.0f * Model.sensorMaxes[ind].absoluteValue) * spacevertical
                    //stopY = end!! * spacevertical/2.0f * Model.sensorMaxes[ind].absoluteValue + paddingVertical + spacevertical/2.0f
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
                    Model.timestamps[i]?.let {
                        // Draw the ticks on the x-axis
                        //var x = i * (this.measuredWidth.toFloat() - paddingHorizontal) / xNum + paddingHorizontal
                        var x = i * spacehorizontal / Model.timestamps.size + paddingHorizontal // We can't divide through 0 here as this is only executed if there is at least one value in timestamps
                        if(it != Model.markerTimestamp) {
                            //canvas?.drawLine(x, xAxisY, x, xAxisY - 5.0f, paint_text)
                            canvas?.drawLine(
                                x,
                                xAxisY+5.0f,
                                x,
                                xAxisY-5.0f,
                                paint_baseline
                            )
                            paint_text.textAlign = Paint.Align.CENTER
                            canvas?.drawText(
                                Model.millisToMinutes(it), // it = Model.timestamps[i]
                                x,
                                xAxisY + 10.0f + paint_text.textSize,
                                paint_text
                            )
                        }
                        else {
                            paint_marker.color = Model.markerColor.toInt()
                            canvas?.drawLine(x, 0.0f, x, xAxisY + 5.0f, paint_marker)
                            paint_text.textAlign = Paint.Align.CENTER
                        }
                    }
                }

                start = datapoint
            }
            if(tag.last() == 'X') {
                val avgY = (yAxisY2 - yAxisY1) / 2
                //val realZero = Model.getAverage()/1000 * spacevertical + paddingVertical
                canvas?.drawLine(0.0f, avgY, 0.0f, avgY, paint)
                //canvas?.drawLine() // Baseline
                //canvas?.drawLine(yAxisX-5.0f, realZero, yAxisX+5.0f, realZero, paint)
            }
        }

        var textWidth = paint_text.measureText(streamTag+"_X")
        var lineSpace = textWidth/2.0f
        paint_text.textAlign = Paint.Align.LEFT
        canvas?.drawText(streamTag+"_X", this.measuredWidth.toFloat()-(textWidth+lineSpace)*3, paint_text.textSize+10.0f, paint_text)
        canvas?.drawLine(this.measuredWidth.toFloat()-(textWidth+lineSpace)*2.0f-3.0f*lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, this.measuredWidth.toFloat()-(textWidth+lineSpace)*2.0f-lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, paint_red)
        canvas?.drawText(streamTag+"_Y", this.measuredWidth.toFloat()-(textWidth+lineSpace)*2, paint_text.textSize+10.0f, paint_text)
        canvas?.drawLine(this.measuredWidth.toFloat()-(textWidth+lineSpace)-3.0f*lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, this.measuredWidth.toFloat()-(textWidth+lineSpace)-lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, paint_green)
        canvas?.drawText(streamTag+"_Z", this.measuredWidth.toFloat()-(textWidth+lineSpace), paint_text.textSize+10.0f, paint_text)
        canvas?.drawLine(this.measuredWidth.toFloat()-3.0f*lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, this.measuredWidth.toFloat()-lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, paint_blue)
    }
}

class ExgDataFragment : Fragment() {

    lateinit var mainHandler : Handler

    var channels: MutableList<LineChart> = mutableListOf()
    var containers: MutableList<LinearLayout> = mutableListOf()
    val maxCharts = 8


    val updateChartDelayed = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                var activeChannels = Model.getActiveChannels()
                if(activeChannels != null) {
                    for (i in 1..maxCharts) {
                        if (activeChannels.contains("Channel_${i}")) {
                            //Log.d("Active channels", "Channel_${i-1}")
                            if (containers[i - 1].isGone) containers[i - 1].visibility = View.VISIBLE
                            channels[i-1].invalidate()
                        }
                        else {
                            if (containers[i - 1].isVisible) containers[i - 1].visibility = View.GONE
                        }
                    }
                }
                else {
                    // If no channels are active, don't draw anything
                    for(c in containers) {
                        if(c.isVisible) c.visibility = View.GONE
                    }
                }
            }
            else {
                // If no device is connected, don't draw anything
                for(c in containers) {
                    if(c.isVisible) c.visibility = View.GONE
                }
            }
            //mainHandler.post(this)
            mainHandler.postDelayed(this, Model.refreshRate)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val baseView = inflater.inflate(R.layout.exg_fragment, container, false)

        // Find channels and their containers
        // I realize this is pretty long and it'd be better to add charts programmatically,
        // but I had some trouble with that and thought it wasn't too important since the
        // amount of channels doesn't exceed 8
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_1))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_1))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_2))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_2))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_3))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_3))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_4))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_4))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_5))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_5))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_6))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_6))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_7))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_7))
        channels.add(baseView.findViewById<LineChart>(R.id.exg_channel_8))
        containers.add(baseView.findViewById<LinearLayout>(R.id.exg_channel_container_8))

        // Add a tag to every channel so we know what data to draw
        for(i in 1..8){
            channels[i-1].streamTag = "Channel_${i}"
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

// This class is very similar to the ExG Fragment (and they should probably share a common parent
// class...)
class SensorDataFragment : Fragment() {

    lateinit var gyroscope: SensorChart
    lateinit var accelerometer: SensorChart
    lateinit var magnetometer: SensorChart

    lateinit var gyroContainer: LinearLayout
    lateinit var accContainer: LinearLayout
    lateinit var magContainer: LinearLayout

    lateinit var mainHandler: Handler

    val updateChartDelayed = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                if(Model.isGyroscopeActive()) {
                    if(!gyroContainer.isVisible) gyroContainer.visibility = View.VISIBLE
                    gyroscope.invalidate()
                }
                else {
                    if(gyroContainer.isVisible) gyroContainer.visibility = View.GONE
                }

                if(Model.isAccelerometerActive()) {
                    if(!accContainer.isVisible) accContainer.visibility = View.VISIBLE
                    accelerometer.invalidate()
                }
                else {
                    if(accContainer.isVisible) accContainer.visibility = View.GONE
                }

                if(Model.isMagnetometerActive()) {
                    if(!magContainer.isVisible) magContainer.visibility = View.VISIBLE
                    magnetometer.invalidate()
                }
                else {
                    if(magContainer.isVisible) magContainer.visibility = View.GONE
                }
            }
            else {
                if(gyroContainer.isVisible) gyroContainer.visibility = View.GONE
                if(accContainer.isVisible) accContainer.visibility = View.GONE
                if(magContainer.isVisible) magContainer.visibility = View.GONE
            }

            //mainHandler.post(this)
            mainHandler.postDelayed(this, Model.refreshRate)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val baseview = inflater.inflate(R.layout.sensors_fragment, container, false)

        gyroscope = baseview.findViewById<SensorChart>(R.id.gyroscope)
        gyroContainer = baseview.findViewById<LinearLayout>(R.id.gyro_channel_container)
        accelerometer = baseview.findViewById<SensorChart>(R.id.accelerometer)
        accContainer = baseview.findViewById<LinearLayout>(R.id.acc_channel_container)
        magnetometer = baseview.findViewById<SensorChart>(R.id.magnetometer)
        magContainer = baseview.findViewById<LinearLayout>(R.id.mag_channel_container)

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

// Class for the last tab, to be filled at a later time (?)
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