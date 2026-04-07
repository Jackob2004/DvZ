package com.jackob.dvz.core.handlers

import org.bukkit.event.EventHandler
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent


/**
 * Handles gameplay mechanics that are shared across multiple game states
 */
class GameplayMechanicsHandler : CoreHandler {

    @EventHandler
    fun onItemCraftEvent(event: CraftItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onAnvilUsage(event: PrepareAnvilEvent) {
        event.result = null
    }

    @EventHandler
    fun onItemEnchant(event: PrepareItemEnchantEvent) {
        event.isCancelled = true
    }
}