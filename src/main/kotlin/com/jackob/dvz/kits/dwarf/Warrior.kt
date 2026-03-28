package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class Warrior(internalName: String, owner: Player) : BaseKit(internalName, owner) {

    init {
        WarriorListener
    }

    object WarriorListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }

}