package com.jackob.dvz.core.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

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

}