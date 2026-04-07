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
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

private const val KIT_SELECTOR_ID = "kit_selector"

/**
 * Responsible for functional side of the lobby.
 *  - Handles lobby tools interactions.
 *  - Provides functionality for setting player to lobby state.
 */
class LobbyStateHandler: Listener {
    lateinit var onKitSelectorOpen: (Player) -> Unit

    private val lobbyTools = listOf(
        createItem(Material.CHEST) {
            name = "<dark_gray><b>Kit selection"
            description = """
                <gray>Use it to select your game kit
            """
            persistentDataContainer.set(LOBBY_TOOL_KEY, PersistentDataType.STRING, KIT_SELECTOR_ID)
        }
    )

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun refreshToLobbyState(player: Player) {
        player.teleport(MapStorage.LOBBY_SPAWN!!)
        player.resetAll()
        player.closeInventory()
        lobbyTools.forEach { tool -> player.inventory.addItem(tool) }
    }

    fun unregisterHandler() {
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun onLobbyToolClick(event: PlayerInteractEvent) {
        val player = event.player
        if (player.world != MapStorage.LOBBY_SPAWN!!.world) return

        val lobbyTool = event.item ?: return
        val itemId = lobbyTool.persistentDataContainer.get(LOBBY_TOOL_KEY, PersistentDataType.STRING)
        if (!event.action.isRightClick) return

        when (itemId) {
            KIT_SELECTOR_ID -> onKitSelectorOpen(player)
            else -> Unit
        }
    }

    companion object {
        private val LOBBY_TOOL_KEY = NamespacedKey(DvZ.INSTANCE, "lobby_tool_id")
    }
}