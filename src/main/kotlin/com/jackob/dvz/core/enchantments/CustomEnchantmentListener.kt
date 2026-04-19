package com.jackob.dvz.core.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class CustomEnchantmentListener : Listener {

    @EventHandler
    fun onItemUse(event: PlayerInteractEvent) {
        val item = event.player.inventory.itemInMainHand
        if (item.type == Material.AIR || item.itemMeta == null) return

        val itemEnchantments = event.player.inventory.itemInMainHand.enchantments

        for ((enchantment, level) in itemEnchantments) {
            val keyString = enchantment.key.toString()

            val customEnchant = CustomEnchantmentRegistry.routerMap[keyString] ?: continue

            if (customEnchant is InteractionEnchantment) {
                customEnchant.handleItemUse(event, level)
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        if (item.itemMeta == null) return

        val itemEnchants = item.enchantments

        for ((enchantment, level) in itemEnchants) {
            val keyString = enchantment.key.toString()

            val customEnchant = CustomEnchantmentRegistry.routerMap[keyString] ?: continue

            if (customEnchant is PlacementEnchantment) {
                customEnchant.handleBlockPlace(event, level)
            }
        }
    }

}