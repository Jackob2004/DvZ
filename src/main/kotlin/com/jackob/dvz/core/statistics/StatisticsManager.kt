package com.jackob.dvz.core.statistics

import com.jackob.dvz.DvZ
import com.jackob.dvz.util.async
import kotlinx.serialization.builtins.ListSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val FETCH_URL = "http://127.0.0.1:9698/api/player/"
private const val SAVE_URL = "http://127.0.0.1:9698/api/players/update"

class StatisticsManager(private val levelManager: PlayerLevelManager) : Listener {

    private val playerStats: ConcurrentHashMap<UUID, PlayerStatistics> = ConcurrentHashMap()

    private val logger = DvZ.INSTANCE.slF4JLogger

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun stopCountingStats() {
        HandlerList.unregisterAll(this)
    }

    fun savePlayersData() {
        val data = playerStats
            .map { (uuid, value) ->
                value.experiencePoints = levelManager.getPlayerXP(uuid)
                value
            }

        async {
            APIClient.put(SAVE_URL, ListSerializer(PlayerStatistics.serializer()), data)
                .onSuccess {
                    logger.info("Players data have been saved successfully!")
                }.onFailure { exception ->
                    logger.error("Players data could not be saved, error: $exception")
                }
        }
    }

    private fun fetchPlayerStats(player: Player) {
        val uuid = player.uniqueId
        val playerName = player.name
        val uuidAsStr = uuid.toString()

        async {
            val fullUrl = FETCH_URL + uuidAsStr
            APIClient.get(fullUrl, PlayerStatistics.serializer())
                .onSuccess { stats ->
                    if (stats != null) {
                        playerStats[uuid] = stats
                        levelManager.savePlayerXPData(uuid, stats.experiencePoints)
                    } else {
                        playerStats[uuid] = PlayerStatistics(uuid = uuidAsStr, name = playerName)
                        levelManager.savePlayerXPData(uuid, 0)
                    }
                }.onFailure { exception ->
                    logger.error("Failed fetching player stats, error: $exception")
                }
        }

    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val player = e.player
        if (!playerStats.containsKey(player.uniqueId)) {
            fetchPlayerStats(e.player)
        }
    }

}