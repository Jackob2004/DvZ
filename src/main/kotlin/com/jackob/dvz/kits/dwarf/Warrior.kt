package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.event.Listener
import java.util.UUID

class Warrior(internalName: String, owner: UUID) : BaseKit(internalName, owner) {

    override val isHero: Boolean = false

    init {
        WarriorListener
    }

    object WarriorListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }

}