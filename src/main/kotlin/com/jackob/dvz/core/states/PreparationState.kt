package com.jackob.dvz.core.states

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.equipment.Compass
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler
import com.jackob.dvz.core.handlers.LobbyRulesHandler
import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.core.objects.DarknessTask
import com.jackob.dvz.core.objects.GoldVault
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
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.scoreboard.Team as BukkitTeam

class PreparationState(
    private val gameMap: GameMap,
    private val selectedKits: Map<UUID, KitType>,
    private val lobbyStateHandler: LobbyStateHandler,
    private val lobbyRulesHandler: LobbyRulesHandler,
    private val gameplayHandler: GameplayMechanicsHandler,
    private val goldVault: GoldVault,
    private val darknessTask: DarknessTask
) : GameState {

    private val customBoard = Bukkit.getScoreboardManager().newScoreboard

    private val dwarfTeam = customBoard.registerNewTeam(Team.DWARF.teamName).apply {
        setAllowFriendlyFire(false)
        setOption(BukkitTeam.Option.COLLISION_RULE, BukkitTeam.OptionStatus.NEVER)
        color(Team.DWARF.color)
    }

    private val onlinePlayers = CopyOnWriteArraySet<Player>()

    private val onlineDwarfs = AtomicInteger()

    private val dwarvenCompass = Compass(generateCompassLocations())

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
                updateLine(3, " <white>${goldVault.getGoldAmount()}")
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

    private fun generateCompassLocations() : List<Compass.NamedLocation> {
        val list = mutableListOf<Compass.NamedLocation>()
        list.add(Compass.NamedLocation("Goldmine", Material.GOLD_BLOCK, gameMap.goldmine))
        list.add(Compass.NamedLocation("Sawmill", Material.IRON_BARS, gameMap.sawmill))
        list.add(Compass.NamedLocation("Oil", Material.SPONGE, gameMap.oil))

        for ((idx, shrine) in gameMap.shrines.withIndex()) {
            list.add(Compass.NamedLocation("Shrine #${idx + 1}", Material.ENCHANTING_TABLE, shrine))
        }

        return list
    }

    override fun onEnter() {
        lobbyStateHandler.onKitSelectorOpen = ::openKitSelectionMenu
        lobbyStateHandler.registerHandler(DvZ.INSTANCE)
        lobbyRulesHandler.registerHandler(DvZ.INSTANCE)
        gameplayHandler.registerHandler(DvZ.INSTANCE)
        DvZ.INSTANCE.server.pluginManager.registerEvents(dwarvenCompass, DvZ.INSTANCE)

        for ((uuid, kitType) in selectedKits) {
            uuid.toPlayer()?.let { player ->
                if (player.isOnline) {
                    addDwarf(player, kitType)
                }
            }
        }
        onlinePlayers.addAll(Bukkit.getOnlinePlayers())
        gameStatusSidebar.sendSidebar(onlinePlayers)

        Bukkit.broadcast("<gray>Preparation phase has started, dwarfs prepare for battle!!!".withPrefix().mm())
        gameMap.dwarfSpawn.world.playSound(gameMap.dwarfSpawn, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f)
        startCountdown()
        darknessTask.startTask(onlinePlayers)

        super.onEnter()
    }

    override fun onLeave() {
        lobbyStateHandler.unregisterHandler()
        lobbyRulesHandler.unregisterHandler()
        gameplayHandler.unregisterHandler()
        darknessTask.stopTask()
        onlinePlayers.clear()
        HandlerList.unregisterAll(dwarvenCompass)

        super.onLeave()
    }

    override fun getPlayerTeam(player: Player): Team? {
        return if (isActiveDwarf(player)) Team.DWARF else null
    }

    private fun isActiveDwarf(player: Player): Boolean {
        return dwarfTeam.hasPlayer(player) && KitsManager.hasKit(player)
    }

    private fun lastActiveInRecruiting(player: Player): Boolean {
        return !dwarfTeam.hasPlayer(player) && selectedKits.containsKey(player.uniqueId)
    }

    private fun addDwarf(player: Player, kitType: KitType) {
        player.resetAll()
        player.scoreboard = customBoard
        dwarfTeam.addPlayer(player)
        player.teleport(gameMap.dwarfSpawn)
        KitsManager.setKit(player, kitType)
        player.inventory.addItem(dwarvenCompass.retrieveItem())
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

    private fun onDwarfDeath(player: Player) {
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * 3, 3, false))
        player.playSound(player.location, Sound.ITEM_BOTTLE_FILL, 1f, 1f)

        val message = """
            <rainbow>Dwarf gods saved you from certain death
            <gray> Want to play as a <dark_red>zombie<reset> ?
            <gray> Type <u><white>/settings<reset><gray> to opt in to die in the coming plague
        """.trimIndent().mm()
        player.sendMessage(message)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        onlinePlayers.add(player)
        player.scoreboard = customBoard
        gameStatusSidebar.sendSidebar(listOf(player))

        if (!isActiveDwarf(player)) {
            lobbyStateHandler.refreshToLobbyState(player)
        } else if (lastActiveInRecruiting(player)) {
            addDwarf(player, selectedKits[player.uniqueId]!!)
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

    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        val wouldDied = player.health - event.finalDamage <= 0
        if (wouldDied) {
            event.isCancelled = true
            onDwarfDeath(player)
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

}