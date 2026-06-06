package com.jackob.dvz.kits.zombie

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.event.Listener
import java.util.UUID

class Zombie (internalName: String, owner: UUID) : BaseKit(internalName, owner) {

    init {
        ZombieListener
    }

    object ZombieListener: Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }
}