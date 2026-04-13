package com.jackob.dvz.core.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when a dwarf collects gold via mining, digging, or rampage kills.
 * @property player The dwarf who collected the gold.
 * @property amount The amount of gold collected. Note: During the Preparation phase,
 * this gold is added to the player's inventory instead of being sent to the shrine.
 */
class DwarfGoldCollectEvent(val player: Player, val amount: Int) : Event() {

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