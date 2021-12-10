package com.example.mentalabexplore

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.mentalab.MentalabCommands

object Model {

    fun scanDevices(applicationContext: Context): Set<String> {
        return MentalabCommands.scan()
    }
}