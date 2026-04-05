package com.jackob.dvz.core.states

import com.jackob.dvz.core.handleLobbyDamage
import com.jackob.dvz.core.handleLobbyInteract
import com.jackob.dvz.core.handleLobbyInvClick
import com.jackob.dvz.core.handleLobbyItemDrop
import com.jackob.dvz.core.handleLobbyToolClick
import com.jackob.dvz.core.refreshToLobbyState
import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.Team
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.ui.PagerMenu
import com.jackob.dvz.ui.Sidebar
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.async
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.scoreboard.Team as BukkitTeam

class PreparationState(private val gameMap: GameMap, private val selectedKits: Map<Player, KitType>) : GameState {

    private val customBoard = Bukkit.getScoreboardManager().newScoreboard

    private val dwarfTeam = customBoard.registerNewTeam(Team.DWARF.teamName).apply {
        setAllowFriendlyFire(false)
        setOption(BukkitTeam.Option.COLLISION_RULE, BukkitTeam.OptionStatus.NEVER)
        color(Team.DWARF.color)
    }

    private val onlinePlayers = CopyOnWriteArraySet<Player>()

    private val onlineDwarfs = AtomicInteger()

    private val gameStatusSidebar =
        Sidebar.create("<shadow:#000000:0.5><b><gradient:#1b4332:#2d6a4f:#74c69d>DWARVES VS ZOMBIES</gradient></b>") {
            line(4)
            line(3, " <gold><b>⛁</b> <gradient:#fde047:#eab308>Gold:</gradient>", " <white>0")
            line(2, " <green><b>🪓</b> <gradient:#ABE2C6:#0DD97D>Dwarves:</gradient>", " <white>0")
            line(1, " <light_purple><b>⌚</b> <gradient:#d8b4fe:#a855f7>Time:</gradient>", " <white>0")
            line(0, "<gray>  ────────────────")
        }

    private fun startCountdown() {
        var timer = ConfigStorage.PREPARATION_COUNTDOWN
        async(period = TimeUnit.SECONDS(1)) {
            with(gameStatusSidebar) {
                updateLine(2, " <white>${onlineDwarfs.get()}")
                updateLine(1, " <white>$timer")
                sendSideBarUpdate(onlinePlayers)
            }

            timer--
            if (timer < 0) {
                cancel()
                // start next phase
            }
        }
    }

    override fun onEnter() {
        for ((player, kitType) in selectedKits) {
            if (player.isOnline) {
                addDwarf(player, kitType)
            }
        }
        onlinePlayers.addAll(Bukkit.getOnlinePlayers())
        gameStatusSidebar.sendSidebar(onlinePlayers)

        Bukkit.broadcast("<gray>Preparation phase has started, dwarfs prepare for battle!!!".withPrefix().mm())
        gameMap.dwarfSpawn.world.playSound(gameMap.dwarfSpawn, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f)
        startCountdown()

        super.onEnter()
    }

    private fun isActiveDwarf(player: Player): Boolean {
        return dwarfTeam.hasPlayer(player) && KitsManager.hasKit(player)
    }

    private fun lastActiveInRecruiting(player: Player): Boolean {
        return !dwarfTeam.hasPlayer(player) && selectedKits.containsKey(player)
    }

    private fun addDwarf(player: Player, kitType: KitType) {
        player.resetAll()
        player.scoreboard = customBoard
        dwarfTeam.addPlayer(player)
        player.teleport(gameMap.dwarfSpawn)
        KitsManager.setKit(player, kitType)
        onlineDwarfs.incrementAndGet()
    }

    private fun openKitSelectionMenu(player: Player) {
        val basicDwarfKits = KitType.entries
            .filter { !it.isHero && it.team == Team.DWARF }
            .map {
                createItem(it.displayData.icon) {
                    name = it.displayData.name
                    lore(it.displayData.description.map(String::mm))
                    persistentDataContainer.set(it.key, PersistentDataType.BOOLEAN, false)
                }
            }

        object : PagerMenu(basicDwarfKits, player, "<gray><b>Select kit") {
            override fun handleClick(slot: Int, player: Player) {
                super.handleClick(slot, player)

                menu.getItem(slot)?.let { item ->
                    val keys = item.persistentDataContainer.keys
                    if (keys.isEmpty()) return@handleClick

                    KitType.getByKey(keys.first())?.let { type ->
                        addDwarf(player, type)
                    }
                }

            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        onlinePlayers.add(player)
        player.scoreboard = customBoard
        gameStatusSidebar.sendSidebar(listOf(player))

        if (!isActiveDwarf(player)) {
            refreshToLobbyState(player)
        } else if (lastActiveInRecruiting(player)) {
            addDwarf(player, selectedKits[player]!!)
        } else {
            onlineDwarfs.incrementAndGet()
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        onlinePlayers.remove(player)
        if (isActiveDwarf(player)) {
            onlineDwarfs.decrementAndGet()
        }
    }

    @EventHandler
    fun onToolClick(event: PlayerInteractEvent) {
        handleLobbyToolClick(event, ::openKitSelectionMenu)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        handleLobbyInteract(event)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onEqInteract(event: InventoryClickEvent) {
        handleLobbyInvClick(event)
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        handleLobbyDamage(event)
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        handleLobbyItemDrop(event)
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

}