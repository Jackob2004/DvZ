package com.jackob.dvz.core.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when shrine takes damage and shield is no longer active
 * @property participants zombies who took part in it
 */
class ShrineDamageEvent(val participants: Collection<Player>) : Event() {
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