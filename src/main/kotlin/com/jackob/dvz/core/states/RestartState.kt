package com.jackob.dvz.core.states

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.handlers.LobbyRulesHandler
import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.ui.Sidebar
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.async
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.sync
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.scheduler.BukkitTask
import java.util.concurrent.CopyOnWriteArraySet

private const val RESTART_STATE_PERMISSION = "dvz.restart.state.interact"

/**
 * Final state responsible for the last countdown that marks the end of the game and server restart.
 * In this state players are unable to interact with anything.
 * Current game summary is displayed at the start of the state.
 */
class RestartState(private val lobbyStateHandler: LobbyStateHandler, private val lobbyRulesHandler: LobbyRulesHandler) :
    GameState {

    private val onlinePlayers = CopyOnWriteArraySet<Player>()

    private val gameStatusSidebar =
        Sidebar.create("<white><b>DWARVES VS ZOMBIES") {
            line(2)
            line(1, " <color:#FF8000>Restarts in:</color>", " <white>0")
            line(0, "<gray>  ────────────────")
        }

    private var countdownTask: BukkitTask? = null

    override fun onEnter() {
        lobbyStateHandler.registerHandler(DvZ.INSTANCE)
        lobbyRulesHandler.registerHandler(DvZ.INSTANCE)
        lobbyStateHandler.onKitSelectorOpen = { }

        Bukkit.getOnlinePlayers().forEach { onlinePlayers.add(it) }
        gameStatusSidebar.sendSidebar(onlinePlayers)
        countdownTask = startCountdown()
        // todo display game summary

        super.onEnter()
    }

    override fun onLeave() {
        val kickMessage = "<gray>Game over! <i>Server is restarting".mm()
        onlinePlayers.forEach {
            it.kick(kickMessage)
        }

        lobbyStateHandler.unregisterHandler()
        lobbyRulesHandler.unregisterHandler()

        countdownTask = null
        onlinePlayers.clear()

        super.onLeave()
        Bukkit.shutdown() // todo later change it to restart
    }

    override fun getPlayerTeam(player: Player): TeamType? = null

    private fun startCountdown(): BukkitTask {
        check(countdownTask == null) { "You cannot start final countdown twice!" }

        var counter = ConfigStorage.RESTART_COUNTDOWN
        return async(period = TimeUnit.SECONDS(1)) {
            with(gameStatusSidebar) {
                updateLine(1, " <white>$counter")
                sendSideBarUpdate(onlinePlayers)
            }

            counter--
            if (counter <= 0) {
                cancel()
                sync {
                    onLeave()
                }
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        onlinePlayers.add(player)
        gameStatusSidebar.sendSidebar(listOf(player))
        lobbyStateHandler.refreshToLobbyState(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        onlinePlayers.remove(event.player)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!event.player.hasPermission(RESTART_STATE_PERMISSION)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEqInteract(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!player.hasPermission(RESTART_STATE_PERMISSION)) {
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
        if (!event.player.hasPermission(RESTART_STATE_PERMISSION)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        lobbyStateHandler.refreshToLobbyState(event.player)
    }
}