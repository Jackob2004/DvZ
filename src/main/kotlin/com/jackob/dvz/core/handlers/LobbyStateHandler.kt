package com.jackob.dvz.core.handlers

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.HotspotManager
import com.jackob.dvz.core.statistics.APIClient.CallResult
import com.jackob.dvz.core.statistics.LeaderboardRow
import com.jackob.dvz.core.statistics.PlayerStatistics
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.ui.Menu
import com.jackob.dvz.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.persistence.PersistentDataType
import org.joml.Matrix4f
import java.util.*

private const val KIT_SELECTOR_ID = "dvz-kit_selector"
private const val PLAYER_STATS_ID = "dvz-player_stats"

/**
 * Responsible for functional side of the lobby.
 *  - Handles lobby tools interactions.
 *  - Provides functionality for setting player to lobby state.
 */
class LobbyStateHandler : CoreHandler {
    lateinit var onKitSelectorOpen: (Player) -> Unit

    private val logger = DvZ.INSTANCE.slF4JLogger

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

    init {
        val leaderboardSpawnLoc = MapStorage.LEADERBOARD_SPAWN
        if (leaderboardSpawnLoc != null) {
            HotspotManager.addHotspot(leaderboardSpawnLoc)
            displayLobbyLeaderboard(leaderboardSpawnLoc.add(0.0, 0.8, 0.0))
        } else {
            logger.info("Leaderboard location was not set in the maps.yml file!!!, use /dvz-set-lobby-loc <name> command")
        }
    }

    fun refreshToLobbyState(player: Player) {
        player.teleport(MapStorage.LOBBY_SPAWN!!)
        player.resetAll()
        player.closeInventory()
        lobbyTools.forEach { tool -> player.inventory.addItem(tool) }
    }

    private fun constructLeaderboard(rows: List<LeaderboardRow>): Component {
        val maxRows = 10
        val builder = StringBuilder()
        builder.append("\n")
        builder.append("<gray>=== <aqua><b>Level Leaderboard <reset><gray>===\n")
        builder.append("\n")

        var coveredRows = 0
        for ((idx, row) in rows.withIndex()) {
            builder.append("<gold>${idx + 1}. <white>${row.name} <gray>- <aqua>${row.level}\n")
            coveredRows++
        }

        while (coveredRows < maxRows) {
            builder.append("<gold>${coveredRows + 1}. <white>? <gray>- <aqua>?\n")
            coveredRows++
        }

        return builder.toString().mm()
    }

    private fun spawnForeground(world: World, location: Location, textComponent: Component) {
        val cardColor = Color.fromRGB(11, 19, 30)

        world.spawn(location, TextDisplay::class.java) {
            it.isPersistent = false
            it.alignment = TextDisplay.TextAlignment.CENTER
            it.isSeeThrough = false
            it.billboard = Display.Billboard.VERTICAL
            it.backgroundColor = cardColor
            it.text(textComponent)
        }
    }

    private fun displayLobbyLeaderboard(location: Location) {
        GameManager.statistics.getLeaderboard().thenAcceptAsync({ leaderboardRows ->
            val world = location.world
            if (leaderboardRows != null) {
                val borderColor = Color.fromRGB(0, 200, 220)
                val textComponent = constructLeaderboard(leaderboardRows)

                world.spawn(location, TextDisplay::class.java) {
                    val matrix = Matrix4f().translate(0f, -0.08f, -0.01f).scale(1.06f, 1.06f, 1.0f)
                    it.isPersistent = false
                    it.isSeeThrough = false
                    it.alignment = TextDisplay.TextAlignment.CENTER
                    it.billboard = Display.Billboard.VERTICAL
                    it.backgroundColor = borderColor

                    it.textOpacity = 0
                    it.text(textComponent)
                    it.setTransformationMatrix(matrix)
                }
                spawnForeground(world, location, textComponent)
            } else {
                spawnForeground(world, location, "<red>Could not display Leaderboard!".mm())
            }
        }, Bukkit.getScheduler().getMainThreadExecutor(DvZ.INSTANCE))
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