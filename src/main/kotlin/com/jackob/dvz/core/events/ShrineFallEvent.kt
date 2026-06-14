package com.jackob.dvz.core.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * @property shrineNumber starts from 0
 */
class ShrineFallEvent(val shrineNumber: Int) : Event() {

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