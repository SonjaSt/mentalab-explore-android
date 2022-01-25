package com.example.mentalabexplore

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.mentalab.MentalabCodec
import com.mentalab.MentalabCommands
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.random.Random

object Model {
    var isConnected = false
    var connectedTo = ""
    var maxElements = 100

    val exgCutoff = 2000
    val accCutoff = 5000
    val gyroCutoff = 5000
    val magCutoff = 100000

    var refreshRate: Long = 100
    var scale_y = 20.0f
    var timeWindow = 20 // in seconds
    //var dataMax = 1.0f

    // This is only used for testing with random data points
    var dataAverage = 1.0f
    var visibleData: Queue<Float> = LinkedList<Float>()

    var channelAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var channelMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var sensorMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
    var sensorAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
    var sensorCutoffs: MutableList<Int> = mutableListOf<Int>(5000, 5000, 100000)
    var startTime: Long? = null
    var lastTime: Long? = null
    var timestamps: LinkedList<Long?> = LinkedList<Long?>()
    var channelData: List<Queue<Float>> = listOf(LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>())

    lateinit var dataMap: Map<String, Queue<Float>>

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

    // TODO: Change the way cutoffs work, take average of last x vals
    fun insertDataFromDevice(s: String) {
        var index = getChannelIndexFromString(s)!!
        var newDataPoint = dataMap.get(s)?.poll()
        //Log.d("MODEL dataMap count", "For string $s: ${dataMap.get(s)?.count()}")
        dataMap.get(s)?.clear()
        //var newDataPoint = (Random.nextFloat() - 0.5f) * 4000
        if(Random.nextInt(100) == 1) newDataPoint = -500000.0f //adds random noise instead
        newDataPoint?.let {
            var cutoff = 0
            when {
                index < 8 -> cutoff = exgCutoff;
                index >= 8 && index < 11 -> cutoff = gyroCutoff;
                index >= 11 && index < 14 -> cutoff = accCutoff;
                index >= 14 -> cutoff = magCutoff;
            }
            if(index == 0) Log.d("DATAPOINT", "${newDataPoint}")
            var newMax = newDataPoint
            if(index > 7 && index < 11) newMax = 0.0f
            if(newDataPoint.absoluteValue > channelMaxes[index].absoluteValue){
                if(channelMaxes[index].absoluteValue != 1.0f) {
                    if ((newDataPoint - channelMaxes[index].absoluteValue).absoluteValue < cutoff) channelMaxes[index] = newDataPoint
                }
                else {
                    channelMaxes[index] = newDataPoint
                }
            }

            if (channelData[index].size >= maxElements) {
                channelData[index].remove()
                if(index == 0) timestamps.remove()
                //channelAverages[index!!] -= oldDatapoint / (channelData[index].size + 1)
            }
            //var newDatapoint = Random.nextFloat() * 1000


            channelData[index].add(newDataPoint)
            if(index == 0) { // only add time for one channel
                var t: Long = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                if (startTime == null) {
                    startTime = t
                    lastTime = 0
                    timestamps.add(0)
                }
                t -= startTime!!
                if (t != lastTime) {
                    timestamps.add(t)
                    lastTime = t
                } else timestamps.add(null)
                Log.d("TIME", t.toString())
            }
            if(channelAverages[index] != 1.0f)
            {
                val diff = (channelAverages[index] - newDataPoint).absoluteValue
                if(diff < cutoff) channelAverages[index] = (channelAverages[index] + newDataPoint)/2.0f
            }
            else {
                channelAverages[index] = newDataPoint
            }
        }
    }

    // TODO: Change the way cutoffs work, take average of last x vals
    fun insertExGDataFromDevice(s: String) {
        var index = getChannelIndexFromString(s)!!
        var newDataPoint = dataMap.get(s)?.poll()
        //Log.d("MODEL dataMap count", "For string $s: ${dataMap.get(s)?.count()}")
        dataMap.get(s)?.clear()
        //var newDataPoint = (Random.nextFloat() - 0.5f) * 4000
        //if(Random.nextInt(100) == 1) newDataPoint = 1000000.0f //adds random noise instead
        newDataPoint?.let {
            if(newDataPoint.absoluteValue > channelMaxes[index].absoluteValue){
                if(channelMaxes[index].absoluteValue != 1.0f) {
                    if ((newDataPoint - channelMaxes[index].absoluteValue).absoluteValue < exgCutoff) channelMaxes[index] = newDataPoint
                }
                else {
                    channelMaxes[index] = newDataPoint
                }
            }

            if (channelData[index].size >= maxElements) {
                channelData[index].remove()
                //channelAverages[index!!] -= oldDatapoint / (channelData[index].size + 1)
            }
            //var newDatapoint = Random.nextFloat() * 1000


            channelData[index].add(newDataPoint)
            if(channelAverages[index] != 1.0f)
            {
                val diff = (channelAverages[index] - newDataPoint).absoluteValue
                if(diff < exgCutoff) channelAverages[index] = (channelAverages[index] + newDataPoint)/2.0f
            }
            else {
                channelAverages[index] = newDataPoint
            }
        }
    }

    fun insertSensorDataFromDevice(s: String) {
        var index = getChannelIndexFromString(s)!!
        var newDataPoint = dataMap.get(s)?.poll()
        //Log.d("MODEL dataMap count", "For string $s: ${dataMap.get(s)?.count()}")
        dataMap.get(s)?.clear()
        //var newDataPoint = (Random.nextFloat() - 0.5f) * 4000
        //if(Random.nextInt(100) == 1) newDataPoint = 1000000.0f //adds random noise instead
        newDataPoint?.let {
            var ind = -1
            when {
                index < 11 -> {
                    ind = 0
                }
                index < 14 -> {
                    ind = 1
                }
                else -> {
                    ind = 2
                }
            }
            if(newDataPoint.absoluteValue > sensorMaxes[ind].absoluteValue){
                if(sensorMaxes[ind].absoluteValue != 1.0f) {
                    if ((newDataPoint - sensorMaxes[ind].absoluteValue).absoluteValue < sensorCutoffs[ind]) sensorMaxes[ind] = newDataPoint
                }
                else {
                    sensorMaxes[ind] = newDataPoint
                }
            }

            if (channelData[index].size >= maxElements) {
                channelData[index].remove()
                //channelAverages[index!!] -= oldDatapoint / (channelData[index].size + 1)
            }
            //var newDatapoint = Random.nextFloat() * 1000


            channelData[index].add(newDataPoint)
            if(sensorAverages[ind] != 1.0f)
            {
                val diff = (sensorAverages[ind] - newDataPoint).absoluteValue
                if(diff < sensorCutoffs[ind]) sensorAverages[ind] = (sensorAverages[ind] + newDataPoint)/2.0f
            }
            else {
                sensorAverages[ind] = newDataPoint
            }
        }
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

    fun getDataAsMap(): Map<String, Queue<Float>> {
        return dataMap
    }

    fun getData(s: String): Queue<Float> {
        val index = getChannelIndexFromString(s)
        index?.let{
            return channelData[index]
        }
        return LinkedList<Float>()
    }

    fun getDeviceKeys(): MutableList<String>? {
        if(isConnected) {
            var keys = mutableListOf<String>()
            for((e, _) in dataMap) {
                keys.add(e)
            }
            return keys
        }
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
        if(!isConnected) return ""
        var temp = dataMap.get("Temperature ")?.poll()
        dataMap.get("Temperature ")?.clear()
        if(temp != null) return "${temp}°C"
        else return ""
    }

    fun getBatteryString(): String {
        if(!isConnected) return ""
        var bat = dataMap.get("Battery ")?.poll()
        dataMap.get("Battery ")?.clear()
        if(bat != null) return "${bat}%"
        else return ""
    }

    fun insertTestData() {
        if(visibleData.size>=maxElements) {
            var oldDatapoint = visibleData.remove()
            dataAverage -= oldDatapoint / (visibleData.size + 1)
        }
        var newDatapoint = Random.nextFloat()*1000
        visibleData.add(newDatapoint)
        if(visibleData.size < maxElements) {
            // If we are here, our Queue hasn't reached full capacity yet
            dataAverage = 0.0f
            for(e in visibleData) {
                dataAverage += e/visibleData.size
            }
        }
        else {
            dataAverage += newDatapoint/maxElements
        }
    }
}