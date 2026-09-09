package com.jackob.dvz.core.handlers

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.statistics.PlayerStatistics
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.ui.Menu
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.core.statistics.APIClient.CallResult
import com.jackob.dvz.util.formatSeconds
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

private const val KIT_SELECTOR_ID = "dvz-kit_selector"
private const val PLAYER_STATS_ID = "dvz-player_stats"

/**
 * Responsible for functional side of the lobby.
 *  - Handles lobby tools interactions.
 *  - Provides functionality for setting player to lobby state.
 */
class LobbyStateHandler : CoreHandler {
    lateinit var onKitSelectorOpen: (Player) -> Unit

    private val lobbyTools = listOf(
        createItem(Material.CHEST) {
            name = "<dark_gray><b>Kit selection"
            description = """
                <gray>Use it to select your game kit
            """
            persistentDataContainer.set(LOBBY_TOOL_KEY, PersistentDataType.STRING, KIT_SELECTOR_ID)
        },
        createItem(Material.COPPER_CHEST) {
            name = "<dark_gray><b>Player statistics"
            description = """
                <gray>Use it to see your stats
            """
            persistentDataContainer.set(LOBBY_TOOL_KEY, PersistentDataType.STRING, PLAYER_STATS_ID)
        }
    )

    fun refreshToLobbyState(player: Player) {
        player.teleport(MapStorage.LOBBY_SPAWN!!)
        player.resetAll()
        player.closeInventory()
        lobbyTools.forEach { tool -> player.inventory.addItem(tool) }
    }

    @EventHandler
    fun onLobbyToolClick(event: PlayerInteractEvent) {
        val player = event.player

        val lobbyTool = event.item ?: return
        val itemId = lobbyTool.persistentDataContainer.get(LOBBY_TOOL_KEY, PersistentDataType.STRING)
        if (!event.action.isRightClick) return

        when (itemId) {
            KIT_SELECTOR_ID -> onKitSelectorOpen(player)
            PLAYER_STATS_ID -> showStatsMenu(player)
            else -> Unit
        }
    }

    companion object {
        private val LOBBY_TOOL_KEY = NamespacedKey(DvZ.INSTANCE, "lobby_tool_id")
        private val websiteMessage = Component.text("To see more stats visit our website: ", NamedTextColor.GREEN)
            .append(
                Component.text("Click here", NamedTextColor.GRAY, TextDecoration.UNDERLINED)
                    .clickEvent(ClickEvent.openUrl("http://localhost:9697/"))
                    .hoverEvent(HoverEvent.showText("Click to open website".mm()))
            )

        private fun generateStatsMenu(stats: PlayerStatistics): InventoryHolder {
            return Menu.create("Statistics of: <gray><i>${stats.name}") {
                pattern(
                    "F_A_Z_K_F",
                    "_S__L__D_",
                    "F__1_2__F"
                )

                val displayLink = { player: Player ->
                    player.sendMessage(websiteMessage)
                    player.playSound(player.location, Sound.BLOCK_BELL_RESONATE, 1f, 1f)
                }

                button('A') {
                    icon = createItem(Material.ZOMBIE_SPAWN_EGG) {
                        name = "<red>AI zombie kills: <gray>${stats.aiZombieKills}"
                    }
                    onClick = { displayLink(it) }
                }

                button('Z') {
                    icon = createItem(Material.ZOMBIE_HEAD) {
                        name = "<dark_red>Player zombie kills: <gray>${stats.zombieKills}"
                    }
                    onClick = { displayLink(it) }
                }

                button('K') {
                    icon = createItem(Material.PLAYER_HEAD) {
                        name = "<dark_red>Dwarf kills: <gray>${stats.dwarfKills}"
                    }
                    onClick = { displayLink(it) }
                }

                button('L') {
                    icon = createItem(Material.NETHER_STAR) {
                        name = "<dark_red>Level: <gray>${stats.experiencePoints / 100}"
                    }
                    onClick = { displayLink(it) }
                }

                button('D') {
                    icon = createItem(Material.ROOTED_DIRT) {
                        name = "<dark_gray>Deaths: <gray>${stats.deaths}"
                    }
                    onClick = { displayLink(it) }
                }

                button('S') {
                    icon = createItem(Material.ENCHANTING_TABLE) {
                        name = "<aqua>Shrine kills: <gray>${stats.shrineKills}"
                    }
                    onClick = { displayLink(it) }
                }

                button('1') {
                    icon = createItem(Material.CLOCK) {
                        name = "<green>Total play time: <gray>${stats.totalPlayTime.formatSeconds()}"
                    }
                    onClick = { displayLink(it) }
                }

                button('2') {
                    icon = createItem(Material.RED_BANNER) {
                        name = "<Gold>Longest time survived: <gray>${stats.longestTimeSurvived.formatSeconds()}"
                    }
                    onClick = { displayLink(it) }
                }

                button('F') {
                    icon = createItem(Material.BLUE_STAINED_GLASS_PANE) {
                        name = "<gray>Info"
                        description = """
                           Click any icon to receive message with a link to see more detailed statistics. 
                        """
                    }
                    onClick = { displayLink(it) }
                }
            }
        }

        fun showStatsMenu(player: Player, searchedPlayerID: UUID = player.uniqueId) {
            player.sendActionBar("<gray>Loading statistics...".mm())
            GameManager.statistics.getPlayerStats(searchedPlayerID).thenAcceptAsync({ callResult ->
                when (callResult) {
                    is CallResult.NotFound -> player.sendMessage("<gray>There are no records of this player.".mm())
                    is CallResult.Failure -> player.sendMessage("<yellow>Unexpected error occurred.".mm())
                    is CallResult.Success -> player.openInventory(generateStatsMenu(callResult.data).inventory)
                }
            }, Bukkit.getScheduler().getMainThreadExecutor(DvZ.INSTANCE))
        }
    }
}