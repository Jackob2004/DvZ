package com.jackob.dvz.core.enchantments

import org.bukkit.event.player.PlayerInteractEvent

interface InteractionEnchantment {
    fun handleItemUse(event: PlayerInteractEvent, level: Int)
}