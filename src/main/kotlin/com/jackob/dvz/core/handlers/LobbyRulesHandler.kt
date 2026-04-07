package com.jackob.dvz.core.handlers

import com.jackob.dvz.DvZ
import com.jackob.dvz.storage.MapStorage
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent

private const val LOBBY_PERMISSION = "dvz.lobby.interact"

/**
 *  Responsible for enforcing lobby related restrictions.
 */
class LobbyRulesHandler : CoreHandler {

    @EventHandler(priority = EventPriority.LOW)
    fun onLobbyInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (player.hasPermission(LOBBY_PERMISSION)) return
        if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onLobbyInvClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (player.hasPermission(LOBBY_PERMISSION)) return
        if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onLobbyDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onLobbyItemDrop(event: PlayerDropItemEvent) {
        val player = event.player
        if (player.hasPermission(LOBBY_PERMISSION)) return
        if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onLobbyFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event as? Player ?: return
        if (player.world != MapStorage.LOBBY_SPAWN!!.world) return
        event.foodLevel = 20
    }

}