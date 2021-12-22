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
    val maxElements = 100
    //var dataMax = 1.0f

    // This is only used for testing with random data points
    var dataAverage = 1.0f
    var visibleData: Queue<Float> = LinkedList<Float>()

    var channelAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f)
    var channelMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f)
    var channelData: List<Queue<Float>> = listOf(LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>(), LinkedList<Float>())

    lateinit var dataMap: Map<String, Queue<Float>>

    fun scanDevices(applicationContext: Context): Set<String> {
        return MentalabCommands.scan()
    }

    fun connectDevice(name: String): Boolean {
        try {
            MentalabCommands.connect(name)
            getDataFromDevice()
            isConnected = true
        }
        catch(e: Exception) {
            Log.e("Model", "Encountered exception in connectDevice(${name})")
            e.printStackTrace()
            isConnected = false
        }
        return isConnected
    }

    fun getDataFromDevice() {
        val stream = MentalabCommands.getRawData()
        dataMap = MentalabCodec.decode(stream)
    }

    fun insertDataFromDevice(s: String) {
        var index = getChannelIndexFromString(s)
        var newDataPoint = dataMap.get(s)?.poll()
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

    fun getChannelIndexFromString(s: String): Int? {
        when(s) {
            "Gyro_X" -> return 0
            "Gyro_Z" -> return 1
            else -> {
                return null
            }
        }
    }

    fun getDataAsMap(): Map<String, Queue<Float>> {
        return dataMap
    }

    fun getData(s: String): Queue<Float> {
        when (s) {
            "Gyro_X" -> return channelData[0]
            "Gyro_Z" -> return channelData[1]
            else -> {
                return LinkedList<Float>()
            }
        }
    }

    fun getAverage(s: String): Float {
        var index = getChannelIndexFromString(s)
        return channelAverages[index!!]
    }

    fun getMax(s: String): Float {
        var index = getChannelIndexFromString(s)
        return channelMaxes[index!!]
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