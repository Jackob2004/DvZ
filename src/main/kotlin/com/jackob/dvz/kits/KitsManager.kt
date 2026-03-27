package com.jackob.dvz.kits

import org.bukkit.entity.Player

object KitsManager {

    private val playerKits: MutableMap<Player, Kit> = HashMap()

    fun setKit(player: Player, type: KitType) {
        if (playerKits.containsKey(player)) return

        playerKits[player] =
            type.kitClass.getConstructor(String::class.java, Player::class.java).newInstance(type.toString().lowercase(), player)
                .apply {
                    onActivate()
                }
    }

    fun unsetKit(player: Player) {
        playerKits[player]?.onDeactivate()
    }

    fun hasKit(player: Player) = playerKits.containsKey(player)

    fun getKit(player: Player) = playerKits[player]
}