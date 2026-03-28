package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class Archer(internalName: String, owner: Player) : BaseKit(internalName, owner) {

    init {
        ArcherListener
    }

    object ArcherListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }

}