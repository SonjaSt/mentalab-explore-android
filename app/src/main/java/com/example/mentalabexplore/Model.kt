package com.example.mentalabexplore

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Switch
import androidx.annotation.RequiresApi
import com.mentalab.MentalabCodec
import com.mentalab.MentalabCommands
import com.mentalab.MentalabConstants
import com.mentalab.RecordSubscriber
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object Model {
    const val keyGyro = "Gyro"
    const val keyMag = "Mag"
    const val keyAcc = "Acc"
    const val keyChannel = "Channel"
    const val keyTemperature = "Temperature"
    const val keyBattery = "Battery"

    var isConnected = false
    var connectedTo = ""

    var refreshRate: Long = 100 // internal refresh rate, used when scheduling chart updates
    var range_y = 2000.0f // range in uV
    var timeWindow = 10 // in seconds
    var timeGap = 2000L // distance of ticks to each other in ms, i.e. 2000 -> 2 seconds between ticks on x-Axis
    var maxElements = timeWindow*1000 / refreshRate.toInt() // max elements to be drawn

    var batteryVals: LinkedList<Float> = LinkedList<Float>()
    var minBattery: Float = 100.0f
    var temperatureVals: LinkedList<Float> = LinkedList<Float>()
    var channelAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var channelMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var sensorMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
    var sensorAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
    var startTime: Long? = null
    var lastTime: Long? = null
    var timestamps: LinkedList<Long> = LinkedList<Long>() // timestamps in milliseconds
    var markerTimestamps: LinkedList<Long> = LinkedList<Long>()
    var markerColor = 0xFFAA0000
    var markerColors: LinkedList<Long> = LinkedList<Long>()
    // channelData holds *all* channel data (ExG and sensors)
    var channelData: List<LinkedList<Float>> = listOf(LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>())

    var dataMap: Map<String, Queue<Float>>? = null

    fun changeTimeWindow(newTime: Int) {
        timeWindow = newTime
        val newMax = newTime*1000 / refreshRate.toInt()
        if(newMax < maxElements) {
            for((i, c) in channelData.withIndex()) {
                if(c.size > newMax) {
                    // TODO: test this and make sure (c.size-newMax) is correct
                    c.subList(0, (c.size-newMax)).clear()
                    // Note: removing elements from the beginning of the channelData lists
                    // will influence the average for the lists with the current implementation!
                }
            }
        }
        maxElements = newMax
    }

    fun scanDevices(applicationContext: Context): Set<String> {
        return MentalabCommands.scan()
    }

    fun connectDevice(name: String): Boolean {
        try {
            MentalabCommands.connect(name)
            getDataFromDevice()
            isConnected = true
            connectedTo = name
        }
        catch(e: Exception) {
            Log.e("Model", "Encountered exception in connectDevice(${name})")
            e.printStackTrace()
            isConnected = false
        }
        return isConnected
    }

    // The dataMap only changes when settings on the device are changed
    fun getDataFromDevice() {
        val stream = MentalabCommands.getRawData()
        dataMap = MentalabCodec.decode(stream)
    }

    // TODO merge insertDataFromDevice and insertSensorDataFromDevice
    // The reason they're separate right now is because the sensor channels share a common average and max
    // TODO: The device sometimes sends out nonsensical values after turning it on (in my tests a bunch of values around 400000)
    // This could mess with the average for a while when the app is started
    fun insertDataFromDevice(s: String) {
        if(!isConnected || dataMap == null) return

        var index = getChannelIndexFromString(s)!!
        var newDataPoint = dataMap!!.get(s)?.poll()

        if(newDataPoint == null) {
            if(channelData[index].isEmpty()) newDataPoint = 0.0f // Emergency dummy value to make sure our timestamps don't go out of sync
            else newDataPoint = channelData[index].last
        }

        dataMap!!.get(s)?.clear()
        if(newDataPoint!!.absoluteValue > channelMaxes[index].absoluteValue){
            channelMaxes[index] = newDataPoint
        }

        var removed: Float? = null
        if (channelData[index].size >= maxElements) {
            removed = channelData[index].remove()
        }

        channelData[index].add(newDataPoint)

        if(channelAverages[index] == 1.0f) channelAverages[index] == newDataPoint
        if(removed == null) {

            var numPoints = channelData[index].size
            if(numPoints <= 1) channelAverages[index] = newDataPoint
            else channelAverages[index] = ((numPoints-1) * channelAverages[index] + newDataPoint) / numPoints
        }
        else channelAverages[index] = channelAverages[index] + newDataPoint / channelData[index].size - removed / channelData[index].size
    }

    fun insertSensorDataFromDevice(s: String) {
        if(!isConnected) return

        var index = getChannelIndexFromString(s)!!
        var newDataPoint = dataMap?.get(s)?.poll()

        if(newDataPoint == null) {
            if(channelData[index].isEmpty()) newDataPoint = 0.0f // Emergency dummy value to make sure our timestamps don't go out of sync
            else newDataPoint = channelData[index].last
        }
        dataMap?.get(s)?.clear()

        var ind = -1
        when {
            index < 11 -> { ind = 0 }
            index < 14 -> { ind = 1 }
            else -> { ind = 2 }
        }

        if(newDataPoint!!.absoluteValue > sensorMaxes[ind].absoluteValue){
            sensorMaxes[ind] = newDataPoint
        }

        var removed: Float? = null
        if (channelData[index].size >= maxElements) {
            removed = channelData[index].remove()
        }

        channelData[index].add(newDataPoint)

        // Note: the sensor averages are pretty useless, so I mimic the behaviour for the ExG data
        // The average ends up being the average over all axes
        if(sensorAverages[ind] == 1.0f) sensorAverages[ind] == newDataPoint
        if(removed == null) {
            var avg = 0.0f
            for (a in channelData[index])
                avg += a
            if(!channelData[index].isEmpty()) avg /= channelData[index].size
            sensorAverages[ind] = avg
        }
        else sensorAverages[ind] = sensorAverages[ind] + newDataPoint / channelData[index].size - removed / channelData[index].size
    }

    // I should use a map, I know
    fun getChannelIndexFromString(s: String): Int? {
        when(s) {
            "Channel_1" -> return 0
            "Channel_2" -> return 1
            "Channel_3" -> return 2
            "Channel_4" -> return 3
            "Channel_5" -> return 4
            "Channel_6" -> return 5
            "Channel_7" -> return 6
            "Channel_8" -> return 7
            "Gyro_X" -> return 8
            "Gyro_Y" -> return 9
            "Gyro_Z" -> return 10
            "Acc_X" -> return 11
            "Acc_Y" -> return 12
            "Acc_Z" -> return 13
            "Mag_X" -> return 14
            "Mag_Y" -> return 15
            "Mag_Z" -> return 16
            else -> {
                return null
            }
        }
    }

    fun getData(s: String): Queue<Float>? {
        if(!isConnected) return null
        val index = getChannelIndexFromString(s)
        index?.let{
            return channelData[index]
        }
        return LinkedList<Float>()
    }

    fun getDeviceKeys(): MutableList<String>? {
        if(!isConnected || dataMap == null) return null
        var keys = mutableListOf<String>()
        for((e, _) in dataMap!!) {
            keys.add(e)
        }
        return keys

        return null
    }

    fun getActiveChannels(): MutableList<String>? {
        var keys = getDeviceKeys()
        if(keys == null) return null

        var activeChannels: MutableList<String> = mutableListOf()
        for(i in 1..keys.size) {
            if(keys[i-1].contains("Channel_")) activeChannels.add(keys[i-1])
        }
        return activeChannels
    }

    fun isGyroscopeActive(): Boolean {
        val keys = getDeviceKeys()
        keys?.let{
            if(keys.contains("Gyro_X") && keys.contains("Gyro_Y") && keys.contains("Gyro_Z")) return true
        }
        return false
    }

    fun isAccelerometerActive(): Boolean {
        val keys = getDeviceKeys()
        keys?.let{
            if(keys.contains("Acc_X") && keys.contains("Acc_Y") && keys.contains("Acc_Z")) return true
        }
        return false
    }

    fun isMagnetometerActive(): Boolean {
        val keys = getDeviceKeys()
        keys?.let{
            if(keys.contains("Mag_X") && keys.contains("Mag_Y") && keys.contains("Mag_Z")) return true
        }
        return false
    }

    fun getAverage(s: String): Float {
        var index = getChannelIndexFromString(s)
        return channelAverages[index!!]
    }

    fun getTemperatureString(): String {
        if(!isConnected || temperatureVals.size == 0) return ""
        var temperature = temperatureVals[0]
        for(t in temperatureVals) {
            temperature = (temperature + t) / 2.0f
        }
        return "${temperature.toInt()}°C"
    }

    fun getBatteryString(): String {
        if(!isConnected || batteryVals.size == 0) return ""
        var battery = 0.0f
        for(b in batteryVals) {
            battery += b
        }
        battery /= batteryVals.size
        if(battery <= minBattery) minBattery = battery
        return "${minBattery.toInt()}%"
    }

    fun timeWithLeadingZero(t: Long): String{
        return when {
            t < 10 -> "0$t"
            else -> "$t"
        }
    }

    fun millisToHours(s: Long): String {
        val s_seconds = s / 1000
        var h = s_seconds / 3600
        var m = (s_seconds % 3600) / 60
        var s = s_seconds % 60

        return "${timeWithLeadingZero(h)}:${timeWithLeadingZero(m)}:${timeWithLeadingZero(s)}"
    }

    fun scaleToVolts(scale: Float): String {
        if(scale >= 2000.0f) return "${(scale/2000.0f).toInt()} mV"
        else return "${(scale/2.0f).toInt()} uV"
    }

    fun scaleToVolts(): String {
        return scaleToVolts(range_y)
    }

    fun insertAllData() {
        if(!isConnected || dataMap == null) return

        var keys = getDeviceKeys()
        if (keys == null) return

        for (k in keys) {
            if(k.contains(keyChannel)) {
                insertDataFromDevice(k)
            }
            if(k.contains(keyAcc) || k.contains(keyGyro) || k.contains(keyMag)) {
                insertSensorDataFromDevice(k)
            }
            if(k.contains(keyBattery)){
                var bat = dataMap!!.get("Battery ")?.poll()
                dataMap!!.get("Battery ")?.clear()
                if(bat != null) {
                    if (batteryVals.size >= 5) {
                        batteryVals.remove()
                    }
                    batteryVals.add(bat)
                }
            }
            if(k.contains(keyTemperature)){
                var bat = dataMap!!.get("Temperature ")?.poll()
                dataMap!!.get("Temperature ")?.clear()
                if(bat != null) {
                    if (temperatureVals.size >= 5) {
                        temperatureVals.remove()
                    }
                    temperatureVals.add(bat)
                }
            }
        }
    }

    fun setMarker(){
        if(startTime != null) markerTimestamps.add(TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis()) - startTime!!)
    }

    fun updateData() {
        insertAllData()

        if(timestamps.size >= maxElements) timestamps.remove()

        var t: Long = TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis())

        if (startTime == null) {
            startTime = t
            lastTime = 0
        }
        t -= startTime!!
        timestamps.add(t)

        if(!markerTimestamps.isEmpty() && markerTimestamps.first < timestamps[0]) markerTimestamps.removeFirst()
    }

    fun clearAllData() {
        isConnected = false
        connectedTo = ""
        batteryVals.clear()
        minBattery = 100.0f

        temperatureVals.clear()
        channelAverages = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
        channelMaxes = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
        sensorMaxes = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
        sensorAverages = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
        startTime = null
        lastTime = null
        timestamps.clear()
        markerTimestamps.clear()
        markerColor = 0xFFAA0000
        markerColors.clear()

        for(i in 0..(channelData.size-1)){
            channelData[i].clear()
        }

        dataMap = null
    }

    fun formatDeviceMemory() {
        MentalabCommands.formatDeviceMemory()
    }

    fun changeRange(newRange: String) {
        var t = (newRange.subSequence(0, newRange.length-2) as String).toFloat()
        if(newRange.contains("uV")) {
            range_y = t*2.0f
        }
        if(newRange.contains("mV")){
            range_y = t*2000.0f
        }
    }

    // Needed for the y-axis scale spinner
    fun rangeToSelection():Int {
        return when(range_y) {
            2.0f -> 0
            10.0f -> 1
            20.0f -> 2
            200.0f -> 3
            400.0f -> 4
            1000.0f -> 5
            2000.0f -> 6
            10000.0f -> 7
            20000.0f -> 8
            200000.0f -> 9
            else -> 6
        }
    }

    // TODO: Make this work - currently not in use
    // This function sets the new enabled channels and modules (in theory)
    // The behaviour of the function isn't predictable, i.e. sending False for channel 1
    // switches off some other channel (?)
    fun setEnabledChannelsAndModules(activeChannelsMap: Map<String, Boolean>, activeModulesMap: Map<String, Boolean>): Boolean {
        Log.d("MODEL_CHANNELS", activeChannelsMap.toString())
        Log.d("MODEL_CHANNELS", activeModulesMap.toString())
        try {
            MentalabCommands.setEnabled(activeChannelsMap)
            Log.d("MODEL_CHANNELS", "Channels set")
            for ((key, value) in activeModulesMap) {
                MentalabCommands.setEnabled(mapOf(key to value))
                Log.d("MODEL_CHANNELS", "Module set")
            }
            //getDataFromDevice()
            return true
        }
        catch(e: Exception) {
            Log.d("MODEL_CHANNELS", e.toString())
            return false
        }

    }

    // I have no idea how to record data, as there is close to 0 documentation on this
    @RequiresApi(Build.VERSION_CODES.Q)
    fun recordData(c: Context) {
        val s: Uri = Uri.parse("")
        var sub : RecordSubscriber = RecordSubscriber.Builder(s, "recordedData", c).build()
        MentalabCommands.record(sub)
    }

    fun pushDataToLSL() {
        MentalabCommands.pushToLsl()
    }
}