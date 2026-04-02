package com.jackob.dvz.kits.dwarf.hero

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class Elf(internalName: String, owner: Player) : BaseKit(internalName, owner) {

    init {
        ElfListener
    }

    object ElfListener: Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }
}
