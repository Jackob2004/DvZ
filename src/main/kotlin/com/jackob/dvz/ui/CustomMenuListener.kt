package com.jackob.dvz.ui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class CustomMenuListener : Listener {
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.currentItem == null) return
        val customMenu = e.inventory.holder as? CustomMenu ?: return

        e.isCancelled = true

        customMenu.handleClick(e.rawSlot, e.whoClicked as Player)
    }
}