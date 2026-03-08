package com.jackob.dvz.core.states

import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.ui.Menu
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.name
import com.jackob.dvz.util.withPrefix
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder

private const val INFO_BAR_MAP = "<gray><b>Map: <reset><gradient:#11998e:#38ef7d><i>"
private const val INFO_BAR_PLAYERS = "<reset><dark_gray><b>| <gray><b>Players: <reset><gradient:#38ef7d:#11998e><i>"

class RecruitingState(var gameMap: GameMap) : GameState {

    val gameInfoBar = BossBar.bossBar(
        "$INFO_BAR_MAP${gameMap.name} ${INFO_BAR_PLAYERS}0/${ConfigStorage.REQUIRED_PLAYERS}".mm(),
        0.0F,
        BossBar.Color.GREEN,
        BossBar.Overlay.NOTCHED_10
    )

    var wasMapRerolled = false

    var playersWaiting = 0

    var teleportOptionsMenu = recreateTeleportOptions()

    override fun onEnter() {
        super.onEnter()
    }

    override fun onLeave() {
        super.onLeave()
    }

    private fun giveLobbyTools(player: Player) {
        val teleportTool = createItem(Material.COMPASS) {
            name = "<white><b>Teleport options"
            description = """
                <gray>Use it to switch between lobby and the current map.
                <gray>Get to know the map better by exploring its key locations.
            """
        }

        player.inventory.addItem(teleportTool)
    }

    private fun recreateTeleportOptions() : InventoryHolder {
        return Menu.create("<white><b>Teleport options") {
            val secondRow = (1..gameMap.shrines.size).joinToString("").padEnd(9, '_')
            pattern(
                "LDZ______",
                secondRow,
                "GSO______"
            )

            for ((index, location) in gameMap.shrines.withIndex()) {
                button(index.plus(1).digitToChar()) {
                    icon = createItem(Material.ENCHANTING_TABLE) {
                        name = "<yellow>Shrine (#${index + 1})"
                        onClick = {
                            it.teleportAsync(location)
                            it.closeInventory()
                        }
                    }
                }
            }

            button('L') {
                icon = createItem(Material.DIRT) {
                    name = "<white>Lobby"
                    onClick = {
                        it.teleport(MapStorage.LOBBY_SPAWN!!)
                        it.closeInventory()
                    }
                }
            }

            button('D') {
                icon = createItem(Material.PLAYER_HEAD) {
                    name = "<dark_green>Dwarf spawn"
                }
                onClick = {
                    it.teleportAsync(gameMap.dwarfSpawn)
                    it.closeInventory()
                }
            }

            button('Z') {
                icon = createItem(Material.ZOMBIE_HEAD) {
                    name = "<dark_red>Zombie spawn"
                    onClick = {
                        it.teleportAsync(gameMap.zombieSpawn)
                        it.closeInventory()
                    }
                }
            }

            button('G') {
                icon = createItem(Material.GOLD_BLOCK) {
                    name = "<gold>Goldmine"
                    onClick = {
                        it.teleportAsync(gameMap.goldmine)
                        it.closeInventory()
                    }
                }
            }

            button('S') {
                icon = createItem(Material.IRON_BARS) {
                    name = "<white>Sawmill"
                    onClick = {
                        it.teleportAsync(gameMap.sawmill)
                        it.closeInventory()
                    }
                }
            }

            button('O') {
                icon = createItem(Material.SPONGE) {
                    name = "<dark_purple>Oil"
                    onClick = {
                        it.teleportAsync(gameMap.oil)
                        it.closeInventory()
                    }
                }
            }

        }
    }

    private fun updateInfoBar() {
        gameInfoBar.name("$INFO_BAR_MAP${gameMap.name} $INFO_BAR_PLAYERS$playersWaiting/${ConfigStorage.REQUIRED_PLAYERS}".mm())
        gameInfoBar.progress((playersWaiting * 100.0F / ConfigStorage.REQUIRED_PLAYERS) / 100.0F)
    }

    /**
     * Applies behavior/options associated to the recruiting state
     */
    private fun refreshPlayer(player: Player) {
        player.teleportAsync(MapStorage.LOBBY_SPAWN!!)
        player.resetAll()
        player.closeInventory()
        giveLobbyTools(player)
        player.showBossBar(gameInfoBar)
    }

    fun performMapChange(newMap: GameMap) {
        gameMap = newMap

        updateInfoBar()
        teleportOptionsMenu = recreateTeleportOptions()
        Bukkit.broadcast("<gray><i>Map was Changed!!!".withPrefix().mm())
        Bukkit.getOnlinePlayers().forEach {
            refreshPlayer(it)
        }
    }

    fun performMapReroll(rerolledMap: GameMap) {
        if (wasMapRerolled) return
        wasMapRerolled = true

        gameMap = rerolledMap

        updateInfoBar()
        teleportOptionsMenu = recreateTeleportOptions()
        Bukkit.broadcast("<gray><i>Map Rerolled!!!".withPrefix().mm())
        Bukkit.getOnlinePlayers().forEach {
            refreshPlayer(it)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        playersWaiting++
        updateInfoBar()
        val player = event.player

        refreshPlayer(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playersWaiting--
        updateInfoBar()
    }

    @EventHandler
    fun onLobbyToolClick(event: PlayerInteractEvent) {
        val lobbyTool = event.item ?: return
        if (!event.action.isRightClick) return

        when (lobbyTool.type) {
            Material.COMPASS -> event.player.openInventory(teleportOptionsMenu.inventory)
            else -> Unit
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onLobbyInteract(event: PlayerInteractEvent) {
        if (!event.player.hasPermission("dvz.lobby.interact")) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        if (!event.player.hasPermission("dvz.lobby.interact")) {
            event.isCancelled = true
        }
    }

}