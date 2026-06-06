package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.event.Listener
import java.util.UUID

class Archer(internalName: String, owner: UUID) : BaseKit(internalName, owner) {

    init {
        ArcherListener
    }

    object ArcherListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }

}