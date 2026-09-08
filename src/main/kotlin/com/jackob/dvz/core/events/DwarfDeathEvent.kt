package com.jackob.dvz.core.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when dwarf dies
 * @property killer zombie player or null if death cause was different
 * @property victim dwarf player
 * @property gameTimeElapsed attack state passed game time in seconds
 */
class DwarfDeathEvent(val killer: Player?, val victim: Player, val gameTimeElapsed: Int) : Event() {
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