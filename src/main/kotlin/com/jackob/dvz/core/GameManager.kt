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

    fun setGameState(gameState: GameState) {
        this.gameState.onLeave()
        this.gameState = gameState
        this.gameState.onEnter()
    }
}