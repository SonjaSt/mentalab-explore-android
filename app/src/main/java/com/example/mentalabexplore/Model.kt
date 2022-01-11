package com.example.mentalabexplore

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.mentalab.MentalabCodec
import com.mentalab.MentalabCommands
import java.util.*
import kotlin.math.absoluteValue
import kotlin.random.Random

object Model {
    var isConnected = false
    var connectedTo = ""
    val maxElements = 100
    //var dataMax = 1.0f

    // This is only used for testing with random data points
    var dataAverage = 1.0f
    var visibleData: Queue<Float> = LinkedList<Float>()

    var channelAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var channelMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var channelData: List<Queue<Float>> = listOf(LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>())

    lateinit var dataMap: Map<String, Queue<Float>>

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

    fun insertDataFromDevice(s: String) {
        var index = getChannelIndexFromString(s)
        var newDataPoint = dataMap.get(s)?.poll()
        //Log.d("MODEL dataMap count", "For string $s: ${dataMap.get(s)?.count()}")
        dataMap.get(s)?.clear()
        //var newDataPoint = (Random.nextFloat() - 0.5f) * 4000
        newDataPoint?.let {
            if(newDataPoint.absoluteValue > channelMaxes[index!!].absoluteValue) channelMaxes[index!!] = newDataPoint
            if (channelData[index!!].size >= maxElements) {
                var oldDatapoint = channelData[index].remove()
                channelAverages[index!!] -= oldDatapoint / (channelData[index].size + 1)
            }
            //var newDatapoint = Random.nextFloat() * 1000

            channelData[index].add(newDataPoint)
            if (channelData[index].size < maxElements) {
                // If we are here, our Queue hasn't reached full capacity yet
                channelAverages[index!!] = 0.0f
                for (e in channelData[index]) {
                    channelAverages[index!!] += e / channelData[index].size
                }
            } else {
                channelAverages[index!!] += newDataPoint / maxElements
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
        Log.d("MODEL", "getTemperatureString: isConnected")
        var temp = dataMap.get("Temperature ")?.poll()
        Log.d("MODEL", "getBatteryString: temp = ${temp}")
        if(temp != null) return "${temp}°C"
        else return ""
    }

    fun getBatteryString(): String {
        if(!isConnected) return ""
        Log.d("MODEL", "getBatteryString: isConnected")
        var bat = dataMap.get("Battery ")?.poll()
        Log.d("MODEL", "getBatteryString: bat = ${bat}")
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