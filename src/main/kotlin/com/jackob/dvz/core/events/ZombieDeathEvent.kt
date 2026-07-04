package com.jackob.dvz.core.events

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when dwarf kills zombie enemy
 * @property killer dwarf player
 * @property victim ai zombie or player
 */
class ZombieDeathEvent(val killer: Player, val victim: LivingEntity) : Event() {
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