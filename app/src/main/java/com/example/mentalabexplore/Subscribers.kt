package com.example.mentalabexplore

import androidx.viewpager.widget.PagerAdapter
import com.mentalab.packets.Packet
import com.mentalab.packets.sensors.exg.EEGPacket
import com.mentalab.service.io.Subscriber
import com.mentalab.utils.constants.Topic

class ExGSubscriber : Subscriber<Void>(Topic.EXG) {
    override fun accept(p: Packet) {
        Model.exg.addAll(p.getData())
    }
}

class OrientationSubscriber : Subscriber<Void>(Topic.ORN) {
    override fun accept(p: Packet) {
        Model.orn.addAll(p.getData())
    }
}

class EnvironmentSubscriber : Subscriber<Void>(Topic.ENVIRONMENT) {
    override fun accept(p: Packet) {
        Model.env.addAll(p.getData())
    }
}

class MarkerSubscriber : Subscriber<Void>(Topic.MARKER) {
    override fun accept(p: Packet) {
        Model.markers.addAll(p.getData())
    }
}