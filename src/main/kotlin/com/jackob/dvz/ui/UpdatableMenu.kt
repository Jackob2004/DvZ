package com.jackob.dvz.ui

import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

private const val SIZE = 36

open class UpdatableMenu(
    private val gap: Int,
    startContents: List<ItemStack>,
    startTitle: String,
    startDataInfo: String
) : CustomMenu {

    protected val menu = Bukkit.createInventory(this, SIZE, startTitle.mm()).apply {
        populateMenu(this, startContents, startDataInfo)
    }

    private fun populateMenu(inv: Inventory, contents: List<ItemStack>, dataInfo: String) {
        inv.setItem(0, createItem(Material.BARRIER) {
            name = "<red>Exit"
        })

        inv.setItem(8, createItem(Material.EXPERIENCE_BOTTLE) {
            name = dataInfo
        })

        var idx = 9 + gap
        for (item in contents) {
            if (idx >= SIZE) throw IllegalStateException("Too many items!!!")

            inv.setItem(idx, item)
            idx += gap
        }
    }

    protected fun updateMenu(newContents: List<ItemStack>, newDataInfo: String) {
        menu.clear()
        populateMenu(menu, newContents, newDataInfo)
    }

    protected open fun exitButtonAction(player: Player) {
        player.closeInventory()
    }

    override fun handleClick(slot: Int, player: Player) {
        if (slot == 0) {
            exitButtonAction(player)
        }
    }

    override fun getInventory(): Inventory = menu

    fun open(player: Player) {
        player.openInventory(menu)
    }
}