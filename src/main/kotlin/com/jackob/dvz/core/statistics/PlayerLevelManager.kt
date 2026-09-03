package com.jackob.dvz.core.statistics

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.DwarfDeathEvent
import com.jackob.dvz.core.events.ShrineDamageEvent
import com.jackob.dvz.core.events.ZombieDeathEvent
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.mm
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val POINTS_PER_LEVEL = 100

class PlayerLevelManager : Listener {

    private val playerExperiencePoints = ConcurrentHashMap<UUID, Int>()

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun getPlayerLevel(player: Player): Int {
        // todo: use fetchPlayerXP method if player has not been added to the map yet
        return playerExperiencePoints.getOrDefault(player.uniqueId, 0) / POINTS_PER_LEVEL
    }

    fun savePlayerXP() {
        TODO("Save player experience points using internal API")
    }

    private fun fetchPlayerXP() {
        TODO("Fetch player experience points from internal API")
    }

    private fun addXP(player: Player, source: PointsSource) {
        val currPoints = playerExperiencePoints.getOrDefault(player.uniqueId, 0)

        val currLevel = currPoints / POINTS_PER_LEVEL
        val newPoints = currPoints + source.pointsAmount
        val newLevel = newPoints / POINTS_PER_LEVEL

        if (newLevel > currLevel) {
            broadcastLevelChange(player, newLevel)
        }

        playerExperiencePoints[player.uniqueId] = newPoints
    }

    private fun broadcastLevelChange(player: Player, newLevel: Int) {
        player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1f, 1f)
        Bukkit.broadcast("<aqua>Congratulations! <gold>${player.name}<reset> has reached level: <gray>$newLevel".mm())
    }

    @EventHandler
    fun onZombieKill(e: ZombieDeathEvent) {
        val source = if (e.victim is Player) PointsSource.ZOMBIE_KILL else PointsSource.AI_ZOMBIE_KILL

        addXP(e.killer, source)
    }

    @EventHandler
    fun onDwarfKill(e: DwarfDeathEvent) {
        val zombiePlayer = e.killer ?: return

        addXP(zombiePlayer, PointsSource.DWARF_KILL)
    }

    @EventHandler
    fun onShrineKill(e: ShrineDamageEvent) {
        for (zombiePlayer in e.participants) {
            addXP(zombiePlayer, PointsSource.SHRINE_KILL)
        }
    }

    fun getPlayerNameColor(player: Player): NamedTextColor = when (GameManager.getPlayerTeam(player)) {
        TeamType.DWARF -> NamedTextColor.GREEN
        TeamType.ZOMBIE -> NamedTextColor.DARK_RED
        else -> NamedTextColor.GRAY
    }

    @EventHandler
    fun onChat(e: AsyncChatEvent) {
        e.renderer(ChatRenderer.viewerUnaware { source, _ , message ->
            val level = getPlayerLevel(source)
            val prefix = "[<aqua>$level<reset>] ".mm()
            val nameComponent = Component.text(source.name, getPlayerNameColor(source))

            prefix.append(nameComponent).append("<reset>: ".mm()).append(message)
        })
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val playerId = event.player.uniqueId

        if (!playerExperiencePoints.containsKey(playerId)) {
            playerExperiencePoints[playerId] = 0 // todo: replace with call to fetchPlayerXP method
        }
    }

    enum class PointsSource(val pointsAmount: Int) {
        DWARF_KILL(10),
        ZOMBIE_KILL(3),
        AI_ZOMBIE_KILL(1),
        SHRINE_KILL(4)
    }
}