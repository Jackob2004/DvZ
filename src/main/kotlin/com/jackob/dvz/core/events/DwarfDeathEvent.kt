package com.jackob.dvz.core.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when dwarf dies
 * @property killer zombie player or null if death cause was different
 * @property victim dwarf player
 */
class DwarfDeathEvent(val killer: Player?, val victim: Player) : Event() {
    companion object {
        val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}