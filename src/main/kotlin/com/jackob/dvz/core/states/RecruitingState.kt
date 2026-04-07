package com.jackob.dvz.core.states

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.HotspotManager
import com.jackob.dvz.core.LobbyRulesHandler
import com.jackob.dvz.core.LobbyStateManager
import com.jackob.dvz.core.objects.HeroPricker
import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.Team
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.ui.Menu
import com.jackob.dvz.ui.PagerMenu
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.withPrefix
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.max

private const val RECRUITING_PERMISSION = "dvz.recruiting.interact"

private const val INFO_BAR_MAP = "<gray><b>Map: <reset><gradient:#11998e:#38ef7d><i>"
private const val INFO_BAR_PLAYERS = "<reset><dark_gray><b>| <gray><b>Players: <reset><gradient:#38ef7d:#11998e><i>"

class RecruitingState(var gameMap: GameMap, private val lobbyStateManager: LobbyStateManager) : GameState {

    private val gameInfoBar = BossBar.bossBar(
        "$INFO_BAR_MAP${gameMap.name} ${INFO_BAR_PLAYERS}0/${ConfigStorage.REQUIRED_PLAYERS}".mm(),
        0.0F,
        BossBar.Color.GREEN,
        BossBar.Overlay.NOTCHED_10
    )

    private val playersWaiting: MutableSet<Player> = HashSet()

    private var teleportOptionsMenu = recreateTeleportOptions()

    private var countdownTask: BukkitTask? = null

    private val selectedKits: MutableMap<UUID, KitType> = HashMap()

    private var heroPicker: HeroPricker? = null

    var countdownTimer = ConfigStorage.RECRUITING_COUNTDOWN

    var wasMapRerolled = false

    private val teleportTool = createItem(Material.COMPASS) {
        name = "<white><b>Teleport options"
        description = """
                <gray>Use it to switch between lobby and the current map.
                <gray>Get to know the map better by exploring its key locations.
            """
    }

    override fun onEnter() {
        loadKeyMapLocations()
        lobbyStateManager.onKitSelectorOpen = ::openKitSelectionMenu

        super.onEnter()
    }

    override fun onLeave() {
        with(gameMap) {
            HotspotManager.removeHotspot(oil, sawmill, goldmine)
        }
        playersWaiting.forEach {
            it.hideBossBar(gameInfoBar)
        }
        playersWaiting.clear()
        countdownTask = null
        heroPicker = null

        super.onLeave()
    }

    private fun includeHeroKits() {
        heroPicker?.let {
            it.retrieveHeroes().forEach { hero ->
                selectedKits[hero.first] = hero.second
            }
        }
    }

    private fun handleCountdownStart() {
        if (countdownTask != null) return
        if (playersWaiting.size < ConfigStorage.REQUIRED_PLAYERS) return

        Bukkit.broadcast("<u><green>Game starts in ${ConfigStorage.RECRUITING_COUNTDOWN} seconds!!!".withPrefix().mm())
        if (HeroPricker.canPickAnyHero(playersWaiting.size) && heroPicker == null) {
            heroPicker = HeroPricker(playersWaiting.map { it.uniqueId })
        }

        countdownTask = sync(period = TimeUnit.SECONDS(1)) {
            val messageSuffix = if (countdownTimer <= 5) "!!!" else ""
            val sound = if (countdownTimer <= 5) Sound.BLOCK_BELL_USE else Sound.ENTITY_EXPERIENCE_ORB_PICKUP
            playersWaiting.forEach {
                it.showTitle(Title.title("<aqua>$countdownTimer$messageSuffix".mm(), "".mm()))
                it.playSound(it.location, sound, 1f, 1f)
            }

            countdownTimer--
            if (countdownTimer < 0) {
                this.cancel()
                includeHeroKits()
                GameManager.setGameState(PreparationState(gameMap, selectedKits, lobbyStateManager, LobbyRulesHandler()))
            }
        }
    }

    private fun handleCountdownCancel() {
        if (countdownTask == null) return
        if (countdownTask!!.isCancelled) return
        if (playersWaiting.size >= ConfigStorage.REQUIRED_PLAYERS) return

        countdownTask!!.cancel()
        countdownTask = null
        countdownTimer = ConfigStorage.RECRUITING_COUNTDOWN
        Bukkit.broadcast("<u><yellow>Game start canceled, not enough players!!!".withPrefix().mm())
    }

    private fun loadKeyMapLocations() = with(gameMap) {
        shrines.forEach { HotspotManager.addHotspot(it) }
        HotspotManager.addHotspot(dwarfSpawn, zombieSpawn, oil, sawmill, goldmine)
    }

    private fun unloadKeyMapLocations() = with(gameMap) {
        shrines.forEach { HotspotManager.removeHotspot(it) }
        HotspotManager.removeHotspot(dwarfSpawn, zombieSpawn, oil, sawmill, goldmine)
    }

    private fun openKitSelectionMenu(player: Player) {
        val playerId = player.uniqueId

        val basicDwarfKits = KitType.entries
            .filter { !it.isHero && it.team == Team.DWARF }
            .map {
                createItem(it.displayData.icon) {
                    name = it.displayData.name
                    lore(it.displayData.description.map(String::mm))
                    persistentDataContainer.set(it.key, PersistentDataType.BOOLEAN, false)

                    if (selectedKits[playerId] != null && selectedKits[playerId] == it) {
                        enchant(Enchantment.UNBREAKING, 10)
                    }
                }
            }

        object : PagerMenu(basicDwarfKits, player, "<gray><b>Select kit") {
            override fun handleClick(slot: Int, player: Player) {
                super.handleClick(slot, player)

                menu.getItem(slot)?.let { item ->
                    val keys = item.persistentDataContainer.keys
                    if (keys.isEmpty()) return@handleClick

                    KitType.getByKey(keys.first())?.let { type ->
                        player.closeInventory()
                        selectedKits[playerId] = type
                        player.sendMessage("${type.displayData.name} <gray>kit selected".withPrefix().mm())
                        player.playSound(player.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)
                    }
                }

            }
        }

    }

    private fun recreateTeleportOptions(): InventoryHolder {
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
                            it.teleport(location)
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
                    it.teleport(gameMap.dwarfSpawn)
                    it.closeInventory()
                }
            }

            button('Z') {
                icon = createItem(Material.ZOMBIE_HEAD) {
                    name = "<dark_red>Zombie spawn"
                    onClick = {
                        it.teleport(gameMap.zombieSpawn)
                        it.closeInventory()
                    }
                }
            }

            button('G') {
                icon = createItem(Material.GOLD_BLOCK) {
                    name = "<gold>Goldmine"
                    onClick = {
                        it.teleport(gameMap.goldmine)
                        it.closeInventory()
                    }
                }
            }

            button('S') {
                icon = createItem(Material.IRON_BARS) {
                    name = "<white>Sawmill"
                    onClick = {
                        it.teleport(gameMap.sawmill)
                        it.closeInventory()
                    }
                }
            }

            button('O') {
                icon = createItem(Material.SPONGE) {
                    name = "<dark_purple>Oil"
                    onClick = {
                        it.teleport(gameMap.oil)
                        it.closeInventory()
                    }
                }
            }

        }
    }

    private fun updateInfoBar() {
        val barProgressLevel = max((playersWaiting.size * 100.0F / ConfigStorage.REQUIRED_PLAYERS) / 100.0F, 1.0F)
        gameInfoBar.name("$INFO_BAR_MAP${gameMap.name} $INFO_BAR_PLAYERS${playersWaiting.size}/${ConfigStorage.REQUIRED_PLAYERS}".mm())
        gameInfoBar.progress(barProgressLevel)
    }

    /**
     * Applies behavior/options associated to the recruiting state
     */
    private fun refreshPlayer(player: Player) {
        lobbyStateManager.refreshToLobbyState(player)
        player.showBossBar(gameInfoBar)
        player.inventory.addItem(teleportTool)
    }

    private fun handleTeleportToolClick(clickedItem: ItemStack?, action: Action, player: Player) {
        if (!action.isRightClick) return

        if (clickedItem == teleportTool) {
            player.openInventory(teleportOptionsMenu.inventory)
        }
    }

    fun performMapChange(newMap: GameMap) {
        unloadKeyMapLocations()
        gameMap = newMap
        loadKeyMapLocations()

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

        unloadKeyMapLocations()
        gameMap = rerolledMap
        loadKeyMapLocations()

        updateInfoBar()
        teleportOptionsMenu = recreateTeleportOptions()
        Bukkit.broadcast("<gray><i>Map Rerolled!!!".withPrefix().mm())
        Bukkit.getOnlinePlayers().forEach {
            refreshPlayer(it)
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        refreshPlayer(player)
        playersWaiting.add(player)
        handleCountdownStart()
        updateInfoBar()
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        playersWaiting.remove(player)
        gameInfoBar.removeViewer(player)
        handleCountdownCancel()
        updateInfoBar()
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        handleTeleportToolClick(event.item, event.action, player)

        if (!event.player.hasPermission(RECRUITING_PERMISSION)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEqInteract(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!player.hasPermission(RECRUITING_PERMISSION)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        if (!event.player.hasPermission(RECRUITING_PERMISSION)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

}