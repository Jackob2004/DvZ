package com.jackob.dvz.core.states

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler
import com.jackob.dvz.core.handlers.LobbyRulesHandler
import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.core.objects.DarknessTask
import com.jackob.dvz.core.objects.GoldVault
import com.jackob.dvz.core.objects.Team
import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.ui.PagerMenu
import com.jackob.dvz.ui.Sidebar
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.async
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
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

    private var countdownTask: BukkitTask? = null

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

        onlinePlayers.addAll(Bukkit.getOnlinePlayers())
        gameStatusSidebar.sendSidebar(onlinePlayers)
        darknessTask.startTask(onlinePlayers)

        // start plague
        Bukkit.broadcast("<gray>Attack phase has started, <dark_red>zombies have been released!!!".withPrefix().mm())
        countdownTask = startCountdown()

        super.onEnter()
    }

    override fun onLeave() {
        TODO("Not yet implemented")
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

    private fun refreshToAttackState(player: Player) {
        player.resetAll()
        player.teleport(gameMap.zombieSpawn) // todo: teleport according to active shrine
        player.gameMode = GameMode.SPECTATOR
    }

    private fun selectKit(player: Player, kitType: KitType) {
        player.resetAll()
        player.teleport(gameMap.zombieSpawn) // todo: teleport according to active shrine
        KitsManager.setKit(player, kitType)

        if (isNewPlayer(player)) {
            zombieTeam.addMember(player)
        }
    }

    private fun createKitSelectionMenu(): PagerMenu {
        val basicZombieKits = KitType.entries
            .filter { !it.isHero && it.team == TeamType.ZOMBIE }
            .map { it.toItem() }

        return object : PagerMenu(basicZombieKits, title = "<gray><b>Select kit") {
            override fun handleClick(slot: Int, player: Player) {
                super.handleClick(slot, player)

                menu.getItem(slot)?.let { item ->
                    val keys = item.persistentDataContainer.keys
                    if (keys.isEmpty()) return@handleClick

                    KitType.getByKey(keys.first())?.let { type ->
                        selectKit(player, type)
                    }
                }

            }
        }
    }

    private fun isActiveZombie(player: Player): Boolean = zombieTeam.hasMember(player) && KitsManager.hasKit(player)

    private fun isActiveDwarf(player: Player): Boolean = dwarfTeam.hasMember(player) && KitsManager.hasKit(player)

    private fun isNewPlayer(player: Player): Boolean = getPlayerTeam(player) == null

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        onlinePlayers.add(player)
        Team.refreshTeamVisibility(player)
        gameStatusSidebar.sendSidebar(listOf(player))

        if (isNewPlayer(player)) {
            lobbyStateHandler.refreshToLobbyState(player)
        } else if (isActiveDwarf(player)) {
            dwarfTeam.increaseOnlineCount()
        } else if (isActiveZombie(player)) {
            zombieTeam.increaseOnlineCount()
        } else {
            refreshToAttackState(player)
            zombieTeam.increaseOnlineCount()
        }
    }

    @EventHandler
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player

        onlinePlayers.remove(player)
        if (isActiveDwarf(player)) {
            dwarfTeam.decreaseOnlineCount()
        } else if (isActiveZombie(player)) {
            zombieTeam.decreaseOnlineCount()
        }
    }

}