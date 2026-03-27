package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.Kit
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class Archer(val internalName: String, val owner: Player) : Kit {

    init {
        ArcherListener
    }

    override fun getName() = internalName

    override fun getPlayer() = owner

    object ArcherListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }

}