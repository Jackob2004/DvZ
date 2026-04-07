package com.jackob.dvz.core

import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.core.states.GameState
import com.jackob.dvz.core.states.RecruitingState
import com.jackob.dvz.kits.Team
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.storage.MapStorage
import org.bukkit.entity.Player
import kotlin.random.Random

object GameManager {

    private var gameState: GameState

    init {
        gameState = RecruitingState(pickRandomMap(), LobbyStateHandler())
        gameState.onEnter()
        HotspotManager.addHotspot(MapStorage.LOBBY_SPAWN!!)
    }

    private fun pickRandomMap(): GameMap {
        val allMapNames = MapStorage.getMapKeys()!!
        val randomTemplateName = allMapNames[Random.nextInt(allMapNames.size)]

        return MapStorage.getMapData(randomTemplateName)!!
    }

    fun setMap(mapKey: String): Boolean {
        val recruitingState = gameState as? RecruitingState ?: return false
        if (!recruitingState.canChangeGameMap()) return false
        if (mapKey !in MapStorage.getMapKeys()!!) return false
        if (mapKey.contains(recruitingState.gameMap.dwarfSpawn.world.name)) return false

        recruitingState.performMapChange(MapStorage.getMapData(mapKey)!!)

        return true
    }

    fun rerollMap(): Boolean {
        val recruitingState = gameState as? RecruitingState ?: return false
        if (!recruitingState.canChangeGameMap()) return false
        if (recruitingState.wasMapRerolled) return false
        val allMapNames = MapStorage.getMapKeys()!!.takeIf { it.size > 1 } ?: return false

        val currentMapWorldName = recruitingState.gameMap.dwarfSpawn.world.name
        val newMapName = allMapNames
            .filterNot { it.contains(currentMapWorldName) }[Random.nextInt(allMapNames.size - 1)]
        recruitingState.performMapReroll(MapStorage.getMapData(newMapName)!!)

        return true
    }

    fun getPlayerTeam(player: Player) : Team? {
        return gameState.getPlayerTeam(player)
    }

    fun setGameState(gameState: GameState) {
        this.gameState.onLeave()
        this.gameState = gameState
        this.gameState.onEnter()
    }
}