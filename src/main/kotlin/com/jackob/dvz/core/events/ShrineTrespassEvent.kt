package com.jackob.dvz.core.events

import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when zombie crosses outer region of inactive shrine
 * @property zombie could be either Player or other LivingEntity
 */
class ShrineTrespassEvent(val zombie: LivingEntity) : Event() {
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