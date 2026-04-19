package com.jackob.dvz.core.enchantments

import org.bukkit.event.block.BlockPlaceEvent

interface PlacementEnchantment {
    fun handleBlockPlace(event: BlockPlaceEvent, level: Int)
}