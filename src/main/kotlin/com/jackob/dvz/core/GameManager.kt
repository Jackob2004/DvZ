package com.jackob.dvz.core

import com.jackob.dvz.core.states.GameState
import com.jackob.dvz.core.states.RecruitingState
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.storage.MapStorage
import kotlin.random.Random

object GameManager {

    private var gameState: GameState

    init {
        gameState = RecruitingState(pickRandomMap())
        gameState.onEnter()
    }

    private fun pickRandomMap(): GameMap {
        val allMapNames = MapStorage.getMapKeys()!!
        val randomTemplateName = allMapNames[Random.nextInt(allMapNames.size)]

        return MapStorage.getMapData(randomTemplateName)!!
    }

    fun setMap(mapKey: String): Boolean {
        val recruitingState = gameState as? RecruitingState ?: return false
        if (mapKey !in MapStorage.getMapKeys()!!) return false
        if (mapKey.contains(recruitingState.gameMap.dwarfSpawn.world.name)) return false

        recruitingState.changeMap(MapStorage.getMapData(mapKey)!!)

        return true
    }

    fun rerollMap(): Boolean {
        val recruitingState = gameState as? RecruitingState ?: return false
        if (recruitingState.wasMapRerolled) return false
        val allMapNames = MapStorage.getMapKeys()!!.takeIf { it.size > 1 } ?: return false

        val currentMapWorldName = recruitingState.gameMap.dwarfSpawn.world.name
        val newMapName = allMapNames.dropWhile { it.contains(currentMapWorldName) }.shuffled().first()
        recruitingState.performMapReroll(MapStorage.getMapData(newMapName)!!)

        return true
    }

    fun setGameState(gameState: GameState) {
        this.gameState.onLeave()
        this.gameState = gameState
        this.gameState.onEnter()
    }
}