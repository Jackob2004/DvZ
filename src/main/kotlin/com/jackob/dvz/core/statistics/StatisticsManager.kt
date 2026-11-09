package com.jackob.dvz.core.statistics

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.DwarfDeathEvent
import com.jackob.dvz.core.events.DwarfRampageEvent
import com.jackob.dvz.core.events.GameOverEvent
import com.jackob.dvz.core.events.ShrineDamageEvent
import com.jackob.dvz.core.events.ZombieDeathEvent
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.async
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap
import kotlinx.serialization.builtins.ListSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import com.jackob.dvz.core.statistics.APIClient.CallResult
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.sync
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

private const val FETCH_URL = "http://127.0.0.1:9698/api/player/"
private const val SAVE_URL = "http://127.0.0.1:9698/api/players/update"
private const val LEADERBOARD_URL = "http://127.0.0.1:9698/api/players/leaderboard"
private const val GAME_INFO_URL = "http://127.0.0.1:9698/api/livegame/update"

class StatisticsManager(private val levelManager: PlayerLevelManager) : Listener {

    private val playerStats: ConcurrentHashMap<UUID, PlayerStatistics> = ConcurrentHashMap()

    private val shootArrows: HashSet<UUID> = HashSet()

    private val rampageStreaks: Object2IntLinkedOpenHashMap<UUID> = Object2IntLinkedOpenHashMap()

    private val loginTimeStamps: Object2LongLinkedOpenHashMap<UUID> = Object2LongLinkedOpenHashMap()

    private val inFlightFetches: ConcurrentHashMap<UUID, CompletableFuture<CallResult<PlayerStatistics>>> =
        ConcurrentHashMap()

    private val logger = DvZ.INSTANCE.slF4JLogger

    private var gameInfoTask: BukkitTask? = null

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun startSendingGameInfo(producer: () -> StatisticsProducer) {
        gameInfoTask = async(delay = TimeUnit.SECONDS(1), period = TimeUnit.SECONDS(1)) {
            APIClient.put(GAME_INFO_URL, GameInfo.serializer(), producer.invoke().produceGameInfo())
        }
    }

    fun stopSendingGameInfo() {
        gameInfoTask?.cancel()
        sync {
            APIClient.put(GAME_INFO_URL, GameInfo.serializer(), GameInfo("End", 0))
        }
    }

    fun stopCountingStats() {
        HandlerList.unregisterAll(this)
    }

    fun savePlayersData() {
        val data = playerStats
            .map { (uuid, value) ->
                val expPoints = levelManager.getPlayerXP(uuid)
                value.experiencePoints = if (expPoints == 0) value.experiencePoints else expPoints

                val playedTime = calcPlayTime(uuid)
                if (playedTime != null) {
                    value.totalPlayTime += playedTime
                }
                value
            }

        async {
            when (val res = APIClient.put(SAVE_URL, ListSerializer(PlayerStatistics.serializer()), data)) {
                is CallResult.Success -> logger.info("Players data have been saved successfully!")
                is CallResult.Failure -> logger.error("Players data could not be saved, error: ${res.error}")
                else -> Unit
            }
        }
    }


    fun getLeaderboard(): CompletableFuture<List<LeaderboardRow>?> {
        return CompletableFuture.supplyAsync({
            when (val res = APIClient.get(LEADERBOARD_URL, ListSerializer(LeaderboardRow.serializer()))) {
                is CallResult.Success -> res.data
                is CallResult.NotFound -> listOf()
                is CallResult.Failure -> {
                    logger.error("Leaderboard data could not be loaded, error: ${res.error}")
                    null
                }
            }
        }, { task -> Bukkit.getScheduler().runTaskAsynchronously(DvZ.INSTANCE, task) })
    }

    fun getPlayerStats(playerUUID: UUID): CompletableFuture<CallResult<PlayerStatistics>> {
        playerStats[playerUUID]?.let {
            return CompletableFuture.completedFuture(CallResult.Success(it))
        }

        val fullURL = FETCH_URL + playerUUID.toString()
        return inFlightFetches.computeIfAbsent(playerUUID) { uuid ->
            CompletableFuture.supplyAsync({
                APIClient.get(fullURL, PlayerStatistics.serializer())
            }, { task -> Bukkit.getScheduler().runTaskAsynchronously(DvZ.INSTANCE, task) })
                .whenComplete { res, throwable ->
                    if (throwable != null) {
                        logger.error("Unexpected error happened: $throwable")
                    } else {
                        when (res) {
                            is CallResult.Success -> playerStats[uuid] = res.data
                            is CallResult.NotFound -> Unit
                            is CallResult.Failure -> logger.error("Failed fetching player stats, error: ${res.error}")
                        }
                    }
                    inFlightFetches.remove(uuid)
                }
        }
    }

    private fun handlePlayerJoin(player: Player) {
        val uuid = player.uniqueId
        val playerName = player.name
        val uuidAsStr = uuid.toString()

        getPlayerStats(uuid).thenAccept { callResult ->
            when (callResult) {
                is CallResult.Success -> levelManager.savePlayerXPData(uuid, callResult.data.experiencePoints)
                is CallResult.NotFound -> {
                    playerStats[uuid] = PlayerStatistics(uuid = uuidAsStr, name = playerName)
                    levelManager.savePlayerXPData(uuid, 0)
                }

                is CallResult.Failure -> Unit
            }
        }
    }

    private fun calcPlayTime(uuid: UUID): Long? {
        val lastLogin = loginTimeStamps.removeLong(uuid)
        if (lastLogin == 0L) return null

        val now = System.currentTimeMillis()
        return (now - lastLogin) / 1000
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val player = e.player
        val uuid = player.uniqueId
        if (!playerStats.containsKey(uuid)) {
            handlePlayerJoin(e.player)
        }

        loginTimeStamps.put(uuid, System.currentTimeMillis())
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        val uuid = e.player.uniqueId
        val timePassedInSeconds = calcPlayTime(uuid) ?: return
        val stats = playerStats[uuid] ?: return

        stats.totalPlayTime += timePassedInSeconds
    }

    @EventHandler
    fun onDwarfKill(e: DwarfDeathEvent) {
        val killer = e.killer
        if (killer != null) {
            playerStats[killer.uniqueId]?.dwarfKills++
        }

        val victimStats = playerStats[e.victim.uniqueId]
        if (victimStats != null) {
            victimStats.longestTimeSurvived = max(victimStats.longestTimeSurvived, e.gameTimeElapsed.toLong())
        }
    }

    @EventHandler
    fun onZombieKill(e: ZombieDeathEvent) {
        val stats = playerStats[e.killer.uniqueId] ?: return

        if (e.victim is Player) {
            stats.zombieKills++
        } else {
            stats.aiZombieKills++
        }
    }

    @EventHandler
    fun onShrineKill(e: ShrineDamageEvent) {
        for (p in e.participants) {
            val stats = playerStats[p.uniqueId] ?: continue
            stats.shrineKills++
        }
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        val stats = playerStats[e.player.uniqueId] ?: return
        stats.deaths++
    }

    @EventHandler
    fun onGameOver(e: GameOverEvent) {
        for (stats in playerStats) {
            stats.value.gamesPlayed++
        }
    }

    @EventHandler
    fun onRampage(e: DwarfRampageEvent) {
        val uuid = e.dwarfPlayer.uniqueId
        val stats = playerStats[uuid] ?: return

        if (e.onStreak) {
            val oldStreak = rampageStreaks.addTo(uuid, 1)
            stats.rampageKillStreak = max(stats.rampageKillStreak, oldStreak + 1)
        } else {
            rampageStreaks.put(uuid, 1)
        }
    }

    @EventHandler
    fun onBowShoot(e: EntityShootBowEvent) {
        val shooter = e.entity as? Player ?: return
        if (e.bow == null) return
        val stats = playerStats[shooter.uniqueId] ?: return
        stats.bowShoots++
        shootArrows.add(e.projectile.uniqueId)
    }

    @EventHandler
    fun onBowHit(e: ProjectileHitEvent) {
        val projectile = e.entity
        if (!shootArrows.remove(projectile.uniqueId)) return
        val hitEntity = e.hitEntity ?: return
        val shooter = projectile.shooter as? Player ?: return
        val stats = playerStats[shooter.uniqueId] ?: return
        val shooterTeam = GameManager.getPlayerTeam(shooter) ?: return

        if (hitEntity is Player && GameManager.getPlayerTeam(hitEntity) != shooterTeam) {
            stats.bowHits++
        } else if (hitEntity.type == AIZombieScheduler.MOB_TYPE && shooterTeam == TeamType.DWARF) {
            stats.bowHits++
        }
    }

}