package com.jackob.dvz.core

import com.jackob.dvz.DvZ
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.name
import com.jackob.dvz.util.resetAll
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

const val LOBBY_PERMISSION = "dvz.lobby.interact"

const val KIT_SELECTOR_ID = "kit_selector"

private val LOBBY_TOOL_KEY = NamespacedKey(DvZ.INSTANCE, "lobby_tool_id")

private val lobbyTools = listOf(
    createItem(Material.CHEST) {
        name = "<dark_gray><b>Kit selection"
        description = """
                <gray>Use it to select your game kit
            """
        persistentDataContainer.set(LOBBY_TOOL_KEY, PersistentDataType.STRING, KIT_SELECTOR_ID)
    }
)

fun refreshToLobbyState(player: Player) {
    player.teleport(MapStorage.LOBBY_SPAWN!!)
    player.resetAll()
    player.closeInventory()
    lobbyTools.forEach { tool -> player.inventory.addItem(tool) }
}

fun handleLobbyToolClick(event: PlayerInteractEvent, openRespectiveKitSelector: (Player) -> Unit) {
    val player = event.player
    if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

    val lobbyTool = event.item ?: return
    val itemId = lobbyTool.persistentDataContainer.get(LOBBY_TOOL_KEY,PersistentDataType.STRING)
    if (!event.action.isRightClick) return

    when (itemId) {
        KIT_SELECTOR_ID -> openRespectiveKitSelector(player)
        else -> Unit
    }
}

fun handleLobbyInteract(event: PlayerInteractEvent) {
    val player = event.player
    if (player.hasPermission(LOBBY_PERMISSION)) return
    if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

    event.isCancelled = true
}

fun handleLobbyInvClick(event: InventoryClickEvent) {
    val player = event.whoClicked as? Player ?: return
    if (player.hasPermission(LOBBY_PERMISSION)) return
    if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

    event.isCancelled = true
}

fun handleLobbyDamage(event: EntityDamageEvent) {
    val player = event.entity as? Player ?: return
    if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

    event.isCancelled = true
}

fun handleLobbyItemDrop(event: PlayerDropItemEvent) {
    val player = event.player
    if (player.hasPermission(LOBBY_PERMISSION)) return
    if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

    event.isCancelled = true
}