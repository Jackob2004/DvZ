package com.jackob.dvz.util

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.collections.set

class CooldownUtil(private val cooldownInMilliseconds: Long) {

    private val cooldownMap: Object2LongOpenHashMap<UUID> = Object2LongOpenHashMap()

    /**
     * Checks whether player is on cooldown and updates it as well
     */
    fun isOnCooldown(player: Player): Boolean {
        val playerId = player.uniqueId
        val currentTimeStamp = System.currentTimeMillis()

        if (!cooldownMap.containsKey(playerId)) {
            cooldownMap.put(playerId, currentTimeStamp)
            return false
        }

        val lastClick = cooldownMap.getLong(playerId)

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
        val lastClick = cooldownMap.getLong(playerId)

        return currentTimeStamp - lastClick < cooldownInMilliseconds
    }

    fun putOnCooldown(player: Player) {
        val playerId = player.uniqueId
        cooldownMap.put(playerId, System.currentTimeMillis())
    }

    fun wasOnCooldown(player: Player): Boolean {
        return cooldownMap.containsKey(player.uniqueId)
    }

    /**
     * @return null if player is not in the map
      */
    fun getRemainingTime(player: Player) : Long? {
        val id = player.uniqueId
        if (!cooldownMap.containsKey(id)) return null

        val currentTimeStamp = System.currentTimeMillis()
        val lastClick = cooldownMap.getLong(id)
        val remainingTime = cooldownInMilliseconds - (currentTimeStamp - lastClick)

        return if (remainingTime <= 0) 0 else remainingTime
    }

}