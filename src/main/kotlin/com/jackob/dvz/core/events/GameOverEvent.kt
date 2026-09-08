package com.jackob.dvz.core.events

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when AttackState ends.
 */
class GameOverEvent(val gameTime: Int) : Event() {
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
