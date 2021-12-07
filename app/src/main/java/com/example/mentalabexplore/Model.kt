package com.example.mentalabexplore

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.mentalab.MentalabCommands

object Model {

    fun scanDevices(applicationContext: Context) {
        val deviceList = MentalabCommands.scan()
        for((i, item) in deviceList.withIndex()) {
            Log.d("Model", "Device $i: $item, ")
        }
        if (deviceList.isEmpty()) Log.d("Model", "No devices inside list.")
        val toast = Toast.makeText(applicationContext, "${deviceList.size} devices found.", Toast.LENGTH_SHORT)
        toast.show()
    }
}