package com.jackob.dvz.kits.zombie

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class Zombie (internalName: String, owner: Player) : BaseKit(internalName, owner) {

    init {
        ZombieListener
    }

    object ZombieListener: Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }
}