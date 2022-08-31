package com.example.mentalabexplore

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Switch
import androidx.annotation.RequiresApi
import com.mentalab.ExploreDevice
import com.mentalab.MentalabCommands
import com.mentalab.service.decode.MentalabCodec
import com.mentalab.service.io.ContentServer
import com.mentalab.service.io.RecordSubscriber
import com.mentalab.utils.ConfigSwitch
import com.mentalab.utils.constants.ConfigProtocol
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
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
    var device: ExploreDevice? = null

    var refreshRate: Long = 100 // internal refresh rate, used when scheduling chart updates
    var range_y = 2000.0f // range in uV
    var timeWindow = 10 // in seconds
    var timeGap =
        2000L // distance of ticks to each other in ms, i.e. 2000 -> 2 seconds between ticks on x-Axis
    var maxElements = timeWindow * 1000 / refreshRate.toInt() // max elements to be drawn

    var batteryVals: LinkedList<Float> = LinkedList<Float>()
    var minBattery: Float = 100.0f
    var temperatureVals: LinkedList<Float> = LinkedList<Float>()
    var channelAverages: MutableList<Float> =
        mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var channelMaxes: MutableList<Float> =
        mutableListOf<Float>(1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f)
    var sensorMaxes: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
    var sensorAverages: MutableList<Float> = mutableListOf<Float>(1.0f, 1.0f, 1.0f)
    var startTime: Long? = null
    var lastTime: Long? = null
    var timestamps: LinkedList<Long> = LinkedList<Long>() // timestamps in milliseconds
    var markerTimestamps: LinkedList<Long> = LinkedList<Long>()
    var markerColor = 0xFFAA0000
    var markerColors: LinkedList<Long> = LinkedList<Long>()

    val exg = Collections.synchronizedList(ArrayList<Float>())
    val orn = Collections.synchronizedList(ArrayList<Float>())
    val env = Collections.synchronizedList(ArrayList<Float>())
    val markers = Collections.synchronizedList(ArrayList<Float>())
    // channelData holds *all* channel data (ExG and sensors)
    var channelData: List<LinkedList<Float>> = listOf(
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>(),
        LinkedList<Float>()
    )

    fun changeTimeWindow(newTime: Int) {
        timeWindow = newTime
        val newMax = newTime * 1000 / refreshRate.toInt()
        if (newMax < maxElements) {
            for ((i, c) in channelData.withIndex()) {
                if (c.size > newMax) {
                    // TODO: test this and make sure (c.size-newMax) is correct
                    c.subList(0, (c.size - newMax)).clear()
                    // Note: removing elements from the beginning of the channelData lists
                    // will influence the average for the lists with the current implementation!
                }
            }
        }
        maxElements = newMax
    }

    fun scanDevices(applicationContext: Context): Set<BluetoothDevice> {
        return MentalabCommands.scan()
    }

    fun connectDevice(name: String): Boolean {
        try {
            device = MentalabCommands.connect(name)
            device!!.acquire()
            subscribe()
            isConnected = true
            connectedTo = name
        } catch (e: Exception) {
            Log.e("Model", "Encountered exception in connectDevice(${name})")
            e.printStackTrace()
            isConnected = false
        }
        return isConnected
    }

    fun subscribe() {
        ContentServer.getInstance().registerSubscriber(ExGSubscriber())
        ContentServer.getInstance().registerSubscriber(OrientationSubscriber())
        ContentServer.getInstance().registerSubscriber(EnvironmentSubscriber())
        ContentServer.getInstance().registerSubscriber(MarkerSubscriber())
    }

    // Gets called every <refresh rate>ms
    // Adds only the latest(!) value from the packets
    fun insertFromDevice(){
        for(i in 0..device!!.channelCount.asInt-1)
        {
            channelData[i].add(exg[exg.size-4+i])
        }
    }

    // TODO merge insertDataFromDevice and insertSensorDataFromDevice
    // The reason they're separate right now is because the sensor channels share a common average and max
    // TODO: The device sometimes sends out nonsensical values after turning it on (in my tests a bunch of values around 400000)
    // This could mess with the average for a while when the app is started
    fun insertChannelDataFromDevice(channel_number: Int) {
        if (!isConnected || device == null) return
        if(exg.isEmpty()) return

        var newDataPoint = exg[exg.size-device!!.channelCount.asInt+channel_number]

        if (newDataPoint == null) {
            if (channelData[channel_number].isEmpty()) newDataPoint =
                0.0f // Emergency dummy value to make sure our timestamps don't go out of sync
            else newDataPoint = channelData[channel_number].last
        }

        if (newDataPoint!!.absoluteValue > channelMaxes[channel_number].absoluteValue) {
            channelMaxes[channel_number] = newDataPoint
        }

        var removed: Float? = null
        if (channelData[channel_number].size >= maxElements) {
            removed = channelData[channel_number].remove()
        }

        channelData[channel_number].add(newDataPoint)

        if (channelAverages[channel_number] == 1.0f) channelAverages[channel_number] == newDataPoint
        if (removed == null) {
            var numPoints = channelData[channel_number].size
            if (numPoints <= 1) channelAverages[channel_number] = newDataPoint
            else channelAverages[channel_number] =
                ((numPoints - 1) * channelAverages[channel_number] + newDataPoint) / numPoints
        } else channelAverages[channel_number] =
            channelAverages[channel_number] + newDataPoint / channelData[channel_number].size - removed / channelData[channel_number].size
    }

    fun insertEXGDataFromDevice() {
        for (i in 0..device!!.channelCount.asInt-1){
            insertChannelDataFromDevice(i)
        }
        exg.clear()
    }

    fun insertSensorDataFromDevice() {
        if (!isConnected || device == null) return
        if (orn.isEmpty()) return

        for (i in 0..8) {
            var newDataPoint = orn[orn.size-9+i]
            if (newDataPoint == null) {
                if (channelData[i+8].isEmpty()) newDataPoint =
                    0.0f // Emergency dummy value to make sure our timestamps don't go out of sync
                else newDataPoint = channelData[i+8].last
            }
            var ind = -1
            when {
                i+8 < 11 -> {
                    ind = 1
                }
                i+8 < 14 -> {
                    ind = 0
                }
                else -> {
                    ind = 2
                }
            }
            if (newDataPoint!!.absoluteValue > sensorMaxes[ind].absoluteValue) {
                sensorMaxes[ind] = newDataPoint
            }
            var removed: Float? = null
            if (channelData[i+8].size >= maxElements) {
                removed = channelData[i+8].remove()
            }
            channelData[i+8].add(newDataPoint)

            // Note: the sensor averages are pretty useless, so I mimic the behaviour for the ExG data
            // The average ends up being the average over all axes
            if (sensorAverages[ind] == 1.0f) sensorAverages[ind] == newDataPoint
            if (removed == null) {
                var avg = 0.0f
                for (a in channelData[i+8])
                    avg += a
                if (!channelData[i+8].isEmpty()) avg /= channelData[i+8].size
                sensorAverages[ind] = avg
            } else sensorAverages[ind] =
                sensorAverages[ind] + newDataPoint / channelData[i+8].size - removed / channelData[i+8].size
        }
        orn.clear()
    }

    fun insertEnvironmentDataFromDevice() {
        if(!Model.isConnected || device == null || env.isEmpty()) return
        var bat = env[env.size - 1]
        if (bat != null) {
            if (batteryVals.size >= 5) {
                batteryVals.remove()
            }
            batteryVals.add(bat)
        }
        var tmp = env[env.size-3]
        if (tmp != null) {
            if (temperatureVals.size >= 5) {
                temperatureVals.remove()
            }
            temperatureVals.add(bat)
        }
        env.clear()
    }


    fun getData(index: Int): Queue<Float>? {
        if (!isConnected) return null
        return channelData[index]
    }

    /** Currently not in use since there's no way to get active channels from the API
    fun getActiveChannels(): MutableList<String>? {
        // I don't think I have access to the channel mask or anything letting me know which channels are turned on

        var activeChannels: MutableList<String> = mutableListOf()
        for (i in 1..keys.size) {
            if (keys[i - 1].contains("Channel_")) activeChannels.add(keys[i - 1])
        }
        return activeChannels
    }*/

    // Stand-in function that returns that all channels are active
    fun getActiveChannels(): MutableList<Boolean>? {
        var activeChannels: MutableList<Boolean> = mutableListOf(true, true, true, true, true, true, true, true)
        if(device == null) activeChannels = mutableListOf(false, false, false, false, false, false, false, false)
        else {
            if(device!!.channelCount.asInt == 4) {
                activeChannels = mutableListOf(true, true, true, true, false, false, false, false)
            }
        }
        return activeChannels
    }

    fun getAverage(index: Int): Float {
        return channelAverages[index]
    }

    fun getTemperatureString(): String {
        if (!isConnected || temperatureVals.size == 0) return ""
        var temperature = temperatureVals[0]
        for (t in temperatureVals) {
            temperature = (temperature + t) / 2.0f
        }
        return "${temperature.toInt()}°C"
    }

    fun getBatteryString(): String {
        if (!isConnected || batteryVals.size == 0) return ""
        var battery = 0.0f
        for (b in batteryVals) {
            battery += b
        }
        battery /= batteryVals.size
        if (battery <= minBattery) minBattery = battery
        return "${minBattery.toInt()}%"
    }

    fun timeWithLeadingZero(t: Long): String {
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
        if (scale >= 2000.0f) return "${(scale / 2000.0f).toInt()} mV"
        else return "${(scale / 2.0f).toInt()} uV"
    }

    fun scaleToVolts(): String {
        return scaleToVolts(range_y)
    }

    fun insertAllData() {
        if (!isConnected || device == null) return

        insertEXGDataFromDevice()
        insertSensorDataFromDevice()
        insertEnvironmentDataFromDevice()
    }

    fun setMarker() {
        if (startTime != null) markerTimestamps.add(TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis()) - startTime!!)
    }

    fun updateData() {
        insertAllData()

        if (timestamps.size >= maxElements) timestamps.remove()

        var t: Long = TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis())

        if (startTime == null) {
            startTime = t
            lastTime = 0
        }
        t -= startTime!!
        timestamps.add(t)

        if (!markerTimestamps.isEmpty() && markerTimestamps.first < timestamps[0]) markerTimestamps.removeFirst()
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

        for (i in 0..(channelData.size - 1)) {
            channelData[i].clear()
        }

        device = null
    }

    fun formatDeviceMemory() {
        var res = device!!.formatMemory()
    }

    fun changeRange(newRange: String) {
        var t = (newRange.subSequence(0, newRange.length - 2) as String).toFloat()
        if (newRange.contains("uV")) {
            range_y = t * 2.0f
        }
        if (newRange.contains("mV")) {
            range_y = t * 2000.0f
        }
    }

    // Needed for the y-axis scale spinner
    fun rangeToSelection(): Int {
        return when (range_y) {
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

    /*
    // TODO: Make this work - currently not in use
    // This function sets the new enabled channels and modules (in theory)
    // The behaviour of the function isn't predictable, i.e. sending False for channel 1
    // switches off some other channel (?)
    fun setEnabledChannelsAndModules(
        activeChannelsMap: Map<String, Boolean>,
        activeModulesMap: Map<String, Boolean>
    ): Boolean {
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
        } catch (e: Exception) {
            Log.d("MODEL_CHANNELS", e.toString())
            return false
        }

    }*/

    /*
    // I have no idea how to record data, as there is close to 0 documentation on this
    @RequiresApi(Build.VERSION_CODES.Q)
    fun recordData(c: Context) {
        val s: Uri = Uri.parse("")
        var sub: RecordSubscriber = RecordSubscriber.Builder(s, "recordedData", c).build()
        MentalabCommands.record(sub)
    }*/

    /*
    fun pushDataToLSL() {
        MentalabCommands.pushToLsl()
    }*/

    // TODO it might not be possible to send on and off commands (should be adressed)
    fun setChannels(l: MutableList<Boolean>) {
        val l_it = l.iterator();
        val c_switches = mutableSetOf<ConfigSwitch>()
        for (i in ConfigProtocol.values()) {
            if (i.isOfType(ConfigProtocol.Type.Module)) {
                continue;
            }
            c_switches.add(ConfigSwitch(i, l_it.next()));
        }
        device!!.setChannels(c_switches)
    }
}