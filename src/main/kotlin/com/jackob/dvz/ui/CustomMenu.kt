package com.jackob.dvz.ui

import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryHolder

interface CustomMenu: InventoryHolder {
    fun handleClick(slot: Int, player: Player)
}