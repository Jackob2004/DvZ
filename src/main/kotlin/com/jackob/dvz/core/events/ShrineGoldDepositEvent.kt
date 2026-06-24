package com.jackob.dvz.core.events

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * An event that can be called during Attack phase when dwarf deposits gold to the currently active shrine
 */
class ShrineGoldDepositEvent(val player: Player, val amount: Int) : Event() {
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