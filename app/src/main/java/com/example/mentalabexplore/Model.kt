package com.example.mentalabexplore

import android.content.Context
import android.util.Log
import com.mentalab.MentalabCodec
import com.mentalab.MentalabCommands
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object Model {
    val keyGyro = "Gyro"
    val keyMag = "Mag"
    val keyAcc = "Acc"
    val keyChannel = "Channel"
    val keyTemperature = "Temperature"
    val keyBattery = "Battery"

    var isConnected = false
    var connectedTo = ""

    var refreshRate: Long = 100 // internal refresh rate, used when scheduling chart updates
    var range_y = 2000.0f // range in uV
    var timeWindow = 10 // in seconds
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
    var timestamps: LinkedList<Long?> = LinkedList<Long?>()
    var markerTimestamp: Long = -1
    var markerColor = 0xFFAA0000
    // channelData holds *all* channel data (ExG and sensors)
    var channelData: List<LinkedList<Float>> = listOf(LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>())

    var dataMap: Map<String, Queue<Float>>? = null

    fun changeTimeWindow(newTime: Int) {
        timeWindow = newTime
        maxElements = newTime*1000 / refreshRate.toInt()
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

        //if(index == 0) Log.d("AVG", "${channelAverages[index]}")
        //if(index == 0) Log.d("DATAPOINT", "${newDataPoint}")
        //if(index == 0) Log.d("MAP", dataMap.toString())
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

    fun getDataAsMap(): Map<String, Queue<Float>>? {
        if(!isConnected) return null
        return dataMap
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

    fun getMax(s: String): Float {
        //Log.d("MODEL string", s)
        var index = getChannelIndexFromString(s)
        return channelMaxes[index!!]
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

    fun secondsToMinutes(s: Long): String {
        var mins = ""
        var secs = ""
        if(s/60 < 10) mins = "0${s/60}" else mins = "${s/60}"
        if(s%60 < 10) secs = "0${s%60}" else secs = "${s%60}"

        return "$mins:$secs"
    }

    fun millisToMinutes(s: Long): String {
        return secondsToMinutes((s/1000).toLong())
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

    fun updateDataCustomTimestamp(){
        insertAllData()
        if(timestamps.contains(markerTimestamp)) timestamps[timestamps.indexOf(markerTimestamp)] = null
        if(timestamps.size >= maxElements) timestamps.remove()

        var t: Long = TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis())
        if (startTime == null) {
            startTime = t
            lastTime = 0
        }
        t -= startTime!!
        markerTimestamp = t
        timestamps.add(t)
    }

    fun updateData() {
        insertAllData()

        if(timestamps.size >= maxElements) timestamps.remove()

        var t: Long = TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis())
        if (startTime == null) {
            startTime = t
            lastTime = 0
            timestamps.add(0)
        }
        t -= startTime!!
        if (t - lastTime!! >= 2000L) {
            timestamps.add(t)
            lastTime = t
        } else timestamps.add(null)
    }

    fun clearAllData() {
        var isConnected = false
        var connectedTo = ""
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
        markerTimestamp = -1
        markerColor = 0xFFAA0000

        for(i in 0..(channelData.size-1)){
            channelData[i].clear()
        }

        dataMap = null
    }
}