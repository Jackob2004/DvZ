package com.jackob.dvz.kits.dwarf.hero

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import org.bukkit.event.Listener
import java.util.UUID

class Elf(internalName: String, owner: UUID) : BaseKit(internalName, owner) {

    override val isHero: Boolean = true

    init {
        ElfListener
    }

    object ElfListener: Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }
}
