package com.jackob.dvz.util

import org.bukkit.entity.Player
import java.util.UUID
import kotlin.collections.set

class CooldownUtil(private val cooldownInMilliseconds: Long) {

    private val cooldownMap: MutableMap<UUID, Long> = HashMap()

    /**
     * Checks whether player is on cooldown and updates it as well
     */
    fun isOnCooldown(player: Player): Boolean {
        val playerId = player.uniqueId

        val currentTimeStamp = System.currentTimeMillis()
        val lastClick = cooldownMap.putIfAbsent(playerId, currentTimeStamp) ?: return false

        if (currentTimeStamp - lastClick > cooldownInMilliseconds) {
            cooldownMap[playerId] = currentTimeStamp
            return false
        }

        return true
    }

    /**
     * Only checks whether player is on cooldown
     */
    fun isOnCooldownSafe(player: Player): Boolean {
        val playerId = player.uniqueId
        if (!cooldownMap.containsKey(playerId)) return false

        val currentTimeStamp = System.currentTimeMillis()
        val lastClick = cooldownMap[playerId]!!

        return currentTimeStamp - lastClick < cooldownInMilliseconds
    }

    fun putOnCooldown(player: Player) {
        val playerId = player.uniqueId
        cooldownMap[playerId] = System.currentTimeMillis()
    }

    fun wasOnCooldown(player: Player): Boolean {
        return cooldownMap.containsKey(player.uniqueId)
    }

}