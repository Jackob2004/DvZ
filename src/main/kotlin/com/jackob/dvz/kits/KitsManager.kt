package com.jackob.dvz.kits

import org.bukkit.entity.Player
import java.util.UUID

object KitsManager {

    private val playerKits: MutableMap<UUID, BaseKit> = HashMap()

    fun setKit(player: Player, type: KitType) {
        if (playerKits.containsKey(player.uniqueId)) return

        playerKits[player.uniqueId] =
            type.kitClass.getConstructor(String::class.java, UUID::class.java).newInstance(type.toString().lowercase(), player.uniqueId)
                .apply {
                    onActivate()
                }
    }

    fun unsetKit(player: Player) {
        playerKits[player.uniqueId]?.onDeactivate()
        playerKits.remove(player.uniqueId)
    }

    fun hasKit(player: Player) = playerKits.containsKey(player.uniqueId)

    fun getKit(player: Player) = playerKits[player.uniqueId]
}