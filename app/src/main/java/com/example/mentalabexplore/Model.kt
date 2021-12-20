package com.example.mentalabexplore

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.mentalab.MentalabCommands
import java.util.*
import kotlin.random.Random

object Model {
    var isConnected = false
    val maxElements = 100
    var dataAverage = 0.0f
    var visibleData: Queue<Float> = LinkedList<Float>()

    fun scanDevices(applicationContext: Context): Set<String> {
        return MentalabCommands.scan()
    }

    fun connectDevice(name: String): Boolean {
        try {
            MentalabCommands.connect(name)
            isConnected = true
        }
        catch(e: Exception) {
            Log.e("Model", "Encountered exception in connectDevice(${name})")
            e.printStackTrace()
            isConnected = false
        }
        return isConnected
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

    fun getData(): Queue<Float> {
        return visibleData
    }

    fun getAverage(): Float {
        return dataAverage
    }
}