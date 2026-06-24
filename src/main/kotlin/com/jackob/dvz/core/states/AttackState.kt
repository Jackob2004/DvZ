package com.jackob.dvz.core.states

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.equipment.Compass
import com.jackob.dvz.core.events.ShrineFallEvent
import com.jackob.dvz.core.events.ShrineTrespassEvent
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler
import com.jackob.dvz.core.handlers.LobbyRulesHandler
import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.core.objects.DarknessTask
import com.jackob.dvz.core.objects.GoldVault
import com.jackob.dvz.core.objects.Plague
import com.jackob.dvz.core.objects.ShrineManager
import com.jackob.dvz.core.objects.Team
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.ui.PagerMenu
import com.jackob.dvz.ui.Sidebar
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.async
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.repair
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.meta.Damageable
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicInteger

class AttackState(
    private val gameMap: GameMap,
    private val lobbyStateHandler: LobbyStateHandler,
    private val lobbyRulesHandler: LobbyRulesHandler,
    private val gameplayHandler: GameplayMechanicsHandler,
    private val goldVault: GoldVault,
    private val darknessTask: DarknessTask,
    private val dwarvenCompass: Compass,
    private val dwarfTeam: Team
) : GameState {

    private val zombieTeam = Team(TeamType.ZOMBIE)

    private val onlinePlayers = CopyOnWriteArraySet<Player>()

    private val killedMonsters = AtomicInteger()

    private val gameStatusSidebar =
        Sidebar.create("<shadow:#000000:0.5><b><gradient:#260002:#5c0005:#9e0006>DWARVES VS ZOMBIES</gradient></b>") {
            line(6)
            line(5, " <gold><b>⛁</b> <gradient:#fde047:#eab308>Gold:</gradient>", " <white>0")
            line(4, " <green><b>🪓</b> <gradient:#ABE2C6:#0DD97D>Dwarves:</gradient>", " <white>0")
            line(3, " <dark_green><b>☣</b> <gradient:#365314:#84cc16>Zombies:</gradient>", " <white>0")
            line(2, " <red><b>⚔</b> <gradient:#7f1d1d:#ef4444>Killed monsters:</gradient>", " <white>0")
            line(1, " <light_purple><b>⌚</b> <gradient:#d8b4fe:#a855f7>Time:</gradient>", " <white>0")
            line(0, "<gray>  ────────────────")
        }

    private val kitSelectionMenu = createKitSelectionMenu()

    private val spectators = mutableSetOf<Player>()

    private var countdownTask: BukkitTask? = null

    private val shrineManager = ShrineManager(gameMap.shrines.size, onlinePlayers, gameMap.zombieSpawn.world)

    private var currentZombieSpawn = gameMap.zombieSpawn

    private fun startCountdown(): BukkitTask {
        var timer = 0
        return async(period = TimeUnit.SECONDS(1)) {
            timer++
            with(gameStatusSidebar) {
                updateLine(5, " <white>${goldVault.getGoldAmount()}")
                updateLine(4, " <white>${dwarfTeam.getOnlineCount()}")
                updateLine(3, " <white>${zombieTeam.getOnlineCount()}")
                updateLine(2, " <white>${killedMonsters.get()}")
                updateLine(1, " <white>$timer")
                sendSideBarUpdate(onlinePlayers)
            }
        }
    }

    override fun onEnter() {
        lobbyStateHandler.onKitSelectorOpen = kitSelectionMenu::open
        lobbyStateHandler.registerHandler(DvZ.INSTANCE)
        lobbyRulesHandler.registerHandler(DvZ.INSTANCE)
        gameplayHandler.registerHandler(DvZ.INSTANCE)
        dwarvenCompass.registerCompass()

        dwarfTeam.recalculateOnlineCount()
        onlinePlayers.addAll(Bukkit.getOnlinePlayers())
        gameStatusSidebar.sendSidebar(onlinePlayers)
        darknessTask.startTask(onlinePlayers)

        // start plague
        Bukkit.broadcast("<gray>Attack phase has started, <dark_red>zombies have been released!!!".withPrefix().mm())
        countdownTask = startCountdown()
        Plague(
            onlinePlayers.filter { dwarfTeam.hasMember(it) }.map { it.uniqueId },
            emptyList(), // todo: import from settings
            ::convertDwarf
        )
        shrineManager.startShrineTicking()
        goldVault.canDirectlyDeposit = false
        dwarfTeam.onMemberQuit = { count -> handleLastDwarfDeath(count) }

        super.onEnter()
    }

    override fun onLeave() {
        lobbyStateHandler.unregisterHandler()
        lobbyRulesHandler.unregisterHandler()
        gameplayHandler.unregisterHandler()
        goldVault.unregisterVault()
        dwarvenCompass.unregisterCompass()

        darknessTask.stopTask()
        shrineManager.stopShrineTicking()
        countdownTask?.cancel()
        countdownTask = null
        onlinePlayers.clear()
        spectators.clear()

        super.onLeave()
    }

    override fun getPlayerTeam(player: Player): TeamType? {
        var type: TeamType? = null
        if (dwarfTeam.hasMember(player)) {
            type = TeamType.DWARF
        } else if (zombieTeam.hasMember(player)) {
            type = TeamType.ZOMBIE
        }

        return type
    }

    private fun convertDwarf(player: Player) {
        player.resetAll()
        dwarfTeam.removeMember(player)
        zombieTeam.addMember(player)
        KitsManager.unsetKit(player)
        KitsManager.setKit(player, KitType.ZOMBIE)
    }

    private fun refreshToAttackState(player: Player) {
        player.resetAll()

        player.gameMode = GameMode.ADVENTURE
        player.allowFlight = true
        player.isFlying = true
        player.isCollidable = false
        player.isInvisible = true
        onlinePlayers.forEach { if (it != player) it.hidePlayer(DvZ.INSTANCE, player) }
        spectators.add(player)
    }

    private fun selectKit(player: Player, kitType: KitType) {
        player.resetAll()
        onlinePlayers.forEach { if (it != player) it.showPlayer(DvZ.INSTANCE, player) }
        spectators.remove(player)
        player.teleport(currentZombieSpawn)
        KitsManager.setKit(player, kitType)

        if (isNewPlayer(player)) {
            zombieTeam.addMember(player)
        }
    }

    private fun createKitSelectionMenu(): PagerMenu {
        val basicZombieKits = KitType.entries
            .filter { !it.isHero && it.team == TeamType.ZOMBIE }
            .map { it.toItem() }

        return object : PagerMenu(basicZombieKits, canDeactivate = true, title = "<gray><b>Select kit") {
            override fun handleClick(slot: Int, player: Player) {
                super.handleClick(slot, player)

                menu.getItem(slot)?.let { item ->
                    val keys = item.persistentDataContainer.keys
                    if (keys.isEmpty()) return@handleClick

                    KitType.getByKey(keys.first())?.let { type ->
                        selectKit(player, type)
                        player.closeInventory()
                    }
                }

            }
        }
    }

    private fun isActiveZombie(player: Player): Boolean = zombieTeam.hasMember(player) && KitsManager.hasKit(player)

    private fun isInactiveZombie(player: Player): Boolean = zombieTeam.hasMember(player)

    private fun isActiveDwarf(player: Player): Boolean = dwarfTeam.hasMember(player) && KitsManager.hasKit(player)

    private fun isNewPlayer(player: Player): Boolean = getPlayerTeam(player) == null

    private fun updatePlayerDisguise(player: Player) {
        val kit = KitsManager.getKit(player) as? Disguisable ?: return
        kit.startDisguise(player)
    }

    private fun handleLastDwarfDeath(count: Int) {
        if (count != 0) return

        Bukkit.broadcast("<red>All dwarfs died!".mm())
        GameManager.setGameState(RestartState(lobbyStateHandler, lobbyRulesHandler))
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        onlinePlayers.add(player)
        Team.refreshTeamVisibility(player)
        gameStatusSidebar.sendSidebar(listOf(player))
        spectators.forEach { spectator -> player.hidePlayer(DvZ.INSTANCE, spectator) }
        updatePlayerDisguise(player)
        shrineManager.addViewer(player)

        if (isNewPlayer(player)) {
            lobbyStateHandler.refreshToLobbyState(player)
        } else if (isActiveDwarf(player)) {
            dwarfTeam.increaseOnlineCount()
        } else if (isActiveZombie(player)) {
            zombieTeam.increaseOnlineCount()
        } else {
            player.teleport(currentZombieSpawn)
            refreshToAttackState(player)
            zombieTeam.increaseOnlineCount()
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        onlinePlayers.remove(player)
        spectators.remove(player)
        shrineManager.removeViewer(player)
        if (isActiveDwarf(player)) {
            dwarfTeam.decreaseOnlineCount()
        } else if (isActiveZombie(player) || isInactiveZombie(player)) {
            zombieTeam.decreaseOnlineCount()
        }

        val kit = KitsManager.getKit(player) as? Disguisable ?: return
        kit.stopDisguise(player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player

        if (isActiveDwarf(player)) {
            dwarfTeam.removeMember(player)
            zombieTeam.addMember(player)
        } else if (isActiveZombie(player)) {
            killedMonsters.incrementAndGet()
        }

        KitsManager.unsetKit(player)
        event.drops.clear()
        event.droppedExp = 0
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        event.respawnLocation = currentZombieSpawn
    }

    @EventHandler
    fun onPlayerPostRespawn(event: PlayerPostRespawnEvent) {
        refreshToAttackState(event.player)
    }

    @EventHandler
    fun onShrineFall(event: ShrineFallEvent) {
        Bukkit.broadcast("<red><b> Shrine ${event.shrineNumber.plus(1)} has fallen!!!".mm())
        // play global sound
        if (shrineManager.activateNextShrine()) {
            currentZombieSpawn = gameMap.shrines[event.shrineNumber]
        } else {
            Bukkit.broadcast("<dark_red><b> Zombies destroyed all shines the age of dwarves is over!!!!".mm())
            GameManager.setGameState(RestartState(lobbyStateHandler, lobbyRulesHandler))
        }
    }

    @EventHandler
    fun onShrineTrespass(event: ShrineTrespassEvent) {
        event.zombie.apply {
            sendMessage("<red><i>You entered shrine protected by dwarves gods!!!".mm())
            world.strikeLightningEffect(location)
            health = 0.0
        }
    }

    @EventHandler
    fun onSpectatorInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (player.gameMode != GameMode.ADVENTURE) return

        if (event.action == Action.LEFT_CLICK_AIR) {
            kitSelectionMenu.open(player)
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onArmorRepair(e: PlayerInteractEvent) {
        val player = e.player
        if (!isActiveDwarf(player)) return
        val item = e.leftClickItem ?: return
        val meta = item.itemMeta as? Damageable ?: return

        if (meta.damage <= 0) return
        if (!goldVault.makeWithdrawal(ConfigStorage.ARMOR_REPAIR_COST)) return

        if (item.repair(10)) {
            player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1f, 1f)
        }
    }

    @EventHandler
    fun onSpectatorAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (damager.gameMode != GameMode.ADVENTURE) return

        event.isCancelled = true
    }

    @EventHandler
    fun onSpectatorDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (player.gameMode != GameMode.ADVENTURE) return

        event.isCancelled = true
    }

    @EventHandler
    fun unItemPickUp(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (GameManager.getPlayerTeam(player) != TeamType.ZOMBIE) return

        event.isCancelled = true
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

}