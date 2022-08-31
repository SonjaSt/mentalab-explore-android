package com.example.mentalabexplore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mentalab.packets.sensors.exg.EEGPacket
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class DataPagerAdapter(fragmentActivity: FragmentActivity, val fragments:ArrayList<Fragment>) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = fragments.size   // Note: This should always be 3 (3 tabs)

    override fun createFragment(position: Int): Fragment = fragments[position]
}

open class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CardView(context, attrs) {
    var streamTag: String = ""
    var index: Int = -1

    var paddingHorizontal = 150.0f
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

    var transform: Matrix = Matrix()

    protected val paint_line = Paint().apply {
        color = 0xff000000.toInt()
        strokeWidth = 2.0f
    }

    protected val paint_text = Paint().apply {
        color = 0xffaaaaaa.toInt()
        strokeWidth = 2.0f
        textAlign = Paint.Align.RIGHT
        textSize = 30.0f
    }

    protected val paint_marker = Paint().apply {
        color = 0xffaa0000.toInt()
        strokeWidth = 2.0f
    }

    protected val paint_baseline = Paint().apply {
        color = 0xff000000.toInt()
        strokeWidth = 2.0f
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Set axis coordinates depending on height and width of our View
        yAxisY2 = this.measuredHeight.toFloat() - paddingVertical
        xAxisX2 = this.measuredWidth.toFloat()
        xAxisY = yAxisY2

        // Available height and width for the chart
        spacehorizontal = this.measuredWidth - paddingHorizontal // only pad the left side
        spacevertical = this.measuredHeight - 2*paddingVertical
    }

    // Draws axes
    override fun onDraw(canvas: Canvas?) {

        canvas?.drawLine(yAxisX, yAxisY1, yAxisX, yAxisY2, paint_line) // y-Axis
        canvas?.drawLine(xAxisX1, xAxisY, xAxisX2, xAxisY, paint_line) // x-Axis
        val avgY = (yAxisY2-yAxisY1)/2 + paddingVertical
        canvas?.drawLine(xAxisX1, avgY, xAxisX2, avgY, paint_baseline) // Horizontal baseline

        if(!Model.isConnected || Model.getData(index) == null) return
        if(Model.timestamps == null || Model.timestamps.isEmpty()) return

        var firstTime = Long.MIN_VALUE
        // search for the tick start time
        for(i in Model.timestamps.first..(Model.timestamps.first+Model.timeGap)) {
            if (i % Model.timeGap < Model.refreshRate*2) {
                firstTime = i
                break
            }
        }
        // draw the ticks on the x-axis with corresponding time stamps
        // Model.timeGap tells us how far apart the ticks should be
        for(i in firstTime..Model.timestamps.last step Model.timeGap) {
            var x =
                (i - Model.timestamps.first) / (Model.timestamps.last - Model.timestamps.first).toFloat() * spacehorizontal + paddingHorizontal
            canvas?.drawLine(x, xAxisY + 5.0f, x, xAxisY - 5.0f, paint_baseline)
            paint_text.textAlign = Paint.Align.CENTER
            canvas?.drawText(Model.millisToHours(i), x, xAxisY + 10.0f + paint_text.textSize, paint_text)
        }

        for((i, m) in Model.markerTimestamps.withIndex()){
            val rangeMin = Model.timestamps.first
            val rangeMax = Model.timestamps.last
            if(m < rangeMin) continue
            // find out x
            var x = ((m - rangeMin) / (rangeMax - rangeMin).toFloat()) * spacehorizontal + paddingHorizontal
            canvas?.drawLine(x, 0.0f, x, this.measuredHeight.toFloat(), paint_marker)
        }
    }
}

class ExGChart(context: Context, attrs: AttributeSet? = null) : ChartView(context, attrs) {

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if(!Model.isConnected || Model.getData(index) == null) return
        if(Model.timestamps == null || Model.timestamps.isEmpty()) return

        paint_text.textAlign = Paint.Align.RIGHT
        canvas?.drawText("(+${Model.scaleToVolts()})", yAxisX-10.0f, yAxisY1+paint_text.textSize, paint_text)
        canvas?.drawText("(-${Model.scaleToVolts()})", yAxisX-10.0f, yAxisY2-5.0f, paint_text)
        paint_text.textAlign = Paint.Align.LEFT
        canvas?.drawText("ch${streamTag.last()}", 10.0f, this.measuredHeight/2.0f, paint_text)

        var xNum = Model.getData(index)!!.size
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
            0.0f, -1.0f/Model.range_y*spacevertical, Model.getAverage(index)/Model.range_y*spacevertical+(spacevertical/2.0f)+paddingVertical,
            0.0f, 0.0f, 1.0f)
        transform.setValues(mVals)

        // TODO: allocate memory at instantiation for max data size
        // In theory, this size can change at runtime up to some fixed max value
        // Make an array of points for the transformation [x1, y1, x2, y2, ..., x_n, y_n]
        var transPoints: FloatArray = FloatArray(2*(Model.getData(index)!!.size))
        for((i, datapoint) in Model.getData(index)!!.withIndex()) {
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
            canvas?.drawLine(start_x, start_y, end_x, end_y, paint_line)
            start_x = transPoints[i]
            start_y = transPoints[i+1]
        }
    }
}

class SensorChart(context: Context, attrs: AttributeSet? = null) : ChartView(context, attrs) {

    val RED = 0xFFFF0000.toInt()
    val GREEN = 0xFF00FF00.toInt()
    val BLUE = 0xFF0000FF.toInt()

    var paint = Paint().apply {
        strokeWidth = 2.0f
    }

    var ind = -1
    var textWidth = 0.0f
    var lineSpace = 0.0f

    lateinit var tags: Array<String>

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        when(streamTag) {
            "Gyro" -> ind = 0
            "Acc" -> ind = 1
            "Mag" -> ind = 2
        }

        textWidth = paint_text.measureText(streamTag+"_X")
        lineSpace = textWidth/2.0f

        tags = arrayOf("${streamTag}_X", "${streamTag}_Y", "${streamTag}_Z")
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if(!Model.isConnected) return
        if(Model.timestamps == null || Model.timestamps.isEmpty()) return

        paint_text.textAlign = Paint.Align.LEFT
        canvas?.drawText("$streamTag", 10.0f, this.measuredHeight/2.0f, paint_text)

        var xNum = Model.getData(index)!!.size
        var mVals = floatArrayOf(
            (spacehorizontal)/xNum, 0.0f, paddingHorizontal,
            0.0f, -spacevertical/(2.0f * Model.sensorMaxes[ind].absoluteValue), spacevertical/2.0f + paddingVertical,
            0.0f, 0.0f, 1.0f)
        transform.setValues(mVals)

        // TODO: allocate memory at instantiation for max data size
        //Make an array of points for the transformation [x1, y1, x2, y2, ..., x_n, y_n]
        var transPoints: Array<FloatArray> = arrayOf(FloatArray(2*(Model.getData(index)!!.size)), FloatArray(2*(Model.getData(index+1)!!.size)), FloatArray(2*(Model.getData(index+2)!!.size)))
        for(j in 0..2) {
            for((i, datapoint) in Model.getData(index+j)!!.withIndex()) {
                transPoints[j].set(i*2, i.toFloat())
                transPoints[j].set(i*2+1, datapoint)
            }
            transform.mapPoints(transPoints[j])

            // TODO: simplify this (use j to determine spacing)
            // draw keys (i.e. Gyro_X + red line)
            when(j) {
                0 -> {
                    paint.setColor(RED)
                    canvas?.drawText(streamTag+"_X", this.measuredWidth.toFloat()-(textWidth+lineSpace)*3, paint_text.textSize+10.0f, paint_text)
                    canvas?.drawLine(this.measuredWidth.toFloat()-(textWidth+lineSpace)*2.0f-3.0f*lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, this.measuredWidth.toFloat()-(textWidth+lineSpace)*2.0f-lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, paint)
                }
                1 -> {
                    paint.setColor(GREEN)
                    canvas?.drawText(streamTag+"_Y", this.measuredWidth.toFloat()-(textWidth+lineSpace)*2, paint_text.textSize+10.0f, paint_text)
                    canvas?.drawLine(this.measuredWidth.toFloat()-(textWidth+lineSpace)-3.0f*lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, this.measuredWidth.toFloat()-(textWidth+lineSpace)-lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, paint)
                }
                2 -> {
                    paint.setColor(BLUE)
                    canvas?.drawText(streamTag+"_Z", this.measuredWidth.toFloat()-(textWidth+lineSpace), paint_text.textSize+10.0f, paint_text)
                    canvas?.drawLine(this.measuredWidth.toFloat()-3.0f*lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, this.measuredWidth.toFloat()-lineSpace/4.0f, (paint_text.textSize/2.0f)+10.0f, paint)
                }
            }

            var start_x = if (transPoints[j].size >= 2) transPoints[j][0] else 0.0f
            var start_y = if (transPoints[j].size >= 2) transPoints[j][1] else 0.0f
            var end_x: Float
            var end_y: Float

            // go through all available points and draw a line to it from the last point
            for(i in 0..(transPoints[j].size-1) step 2) {
                end_x = transPoints[j][i]
                end_y = transPoints[j][i+1]
                canvas?.drawLine(start_x, start_y, end_x, end_y, paint)
                start_x = transPoints[j][i]
                start_y = transPoints[j][i+1]
            }
        }
        paint_text.textAlign = Paint.Align.RIGHT
        val range = Model.sensorMaxes[ind].roundToInt().absoluteValue
        canvas?.drawText("+$range", yAxisX-10.0f, yAxisY1, paint_text)
        canvas?.drawText("0", yAxisX-10.0f, this.measuredHeight/2.0f, paint_text)
        canvas?.drawText("-$range", yAxisX-10.0f, yAxisY2, paint_text)
    }
}

class ExgDataFragment : Fragment() {

    lateinit var mainHandler : Handler

    var channels: MutableList<ExGChart> = mutableListOf()
    val maxCharts = 8


    val updateChartDelayed = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                var activeChannels = Model.getActiveChannels()
                if(activeChannels != null) {
                    for (i in 1..maxCharts) {
                        if (activeChannels[i-1]) {
                            if (channels[i - 1].isGone) channels[i - 1].visibility = View.VISIBLE
                            channels[i-1].invalidate()
                        }
                        else {
                            if (channels[i - 1].isVisible) channels[i - 1].visibility = View.GONE
                        }
                    }
                }
                else {
                    // If no channels are active, don't draw anything
                    for(c in channels) {
                        if(c.isVisible) c.visibility = View.GONE
                    }
                }
            }
            else {
                // If no device is connected, don't draw anything
                for(c in channels) {
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
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_1))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_2))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_3))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_4))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_5))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_6))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_7))
        channels.add(baseView.findViewById<ExGChart>(R.id.exg_channel_8))

        // Add a tag to every channel so we know what data to draw
        for(i in 1..8){
            channels[i-1].streamTag = "Channel_${i}"
            channels[i-1].index = i-1
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

    lateinit var mainHandler: Handler

    val updateChartDelayed = object : Runnable {
        override fun run() {
            if(Model.isConnected) {
                if(!gyroscope.isVisible) gyroscope.visibility = View.VISIBLE
                gyroscope.invalidate()
                if(!accelerometer.isVisible) accelerometer.visibility = View.VISIBLE
                accelerometer.invalidate()
                if(!magnetometer.isVisible) magnetometer.visibility = View.VISIBLE
                magnetometer.invalidate()
                /**
                if(Model.isGyroscopeActive()) {
                    if(!gyroscope.isVisible) gyroscope.visibility = View.VISIBLE
                    gyroscope.invalidate()
                }
                else {
                    if(gyroscope.isVisible) gyroscope.visibility = View.GONE
                }

                if(Model.isAccelerometerActive()) {
                    if(!accelerometer.isVisible) accelerometer.visibility = View.VISIBLE
                    accelerometer.invalidate()
                }
                else {
                    if(accelerometer.isVisible) accelerometer.visibility = View.GONE
                }

                if(Model.isMagnetometerActive()) {
                    if(!magnetometer.isVisible) magnetometer.visibility = View.VISIBLE
                    magnetometer.invalidate()
                }
                else {
                    if(magnetometer.isVisible) magnetometer.visibility = View.GONE
                }
                */
            }
            else {
                if(gyroscope.isVisible) gyroscope.visibility = View.GONE
                if(accelerometer.isVisible) accelerometer.visibility = View.GONE
                if(magnetometer.isVisible) magnetometer.visibility = View.GONE
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
        accelerometer = baseview.findViewById<SensorChart>(R.id.accelerometer)
        magnetometer = baseview.findViewById<SensorChart>(R.id.magnetometer)

        gyroscope.streamTag = "Gyro"
        gyroscope.index = 14
        accelerometer.streamTag = "Acc"
        accelerometer.index = 8
        magnetometer.streamTag = "Mag"
        magnetometer.index = 11

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