package com.jackob.dvz.core.events

import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when AI Zombies are spawned around a zombie player
 */
class AIZombieSpawnEvent(val zombies: List<LivingEntity>, val zombiePlayer: Player) : Event(){
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