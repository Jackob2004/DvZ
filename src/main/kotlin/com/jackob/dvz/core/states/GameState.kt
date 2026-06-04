package com.jackob.dvz.core.states

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.TeamType
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

sealed interface GameState : Listener {
    fun onEnter() {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }
    fun onLeave() {
        HandlerList.unregisterAll(this)
    }

    fun getPlayerTeam(player: Player) : TeamType?
}