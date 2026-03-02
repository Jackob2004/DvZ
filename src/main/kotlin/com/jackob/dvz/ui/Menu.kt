package com.jackob.dvz.ui

import com.jackob.dvz.util.mm
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

private const val CELLS = 9

class Menu private constructor() {

    private val buttons: HashMap<Char, Button> = HashMap()

    private val rows = mutableListOf<CharArray>()

    fun pattern(vararg rows: String) {
        require(rows.all { it.length == CELLS } && rows.size <= 6)
        rows.map { it.toCharArray() }.forEach { this.rows.add(it) }
    }

    fun button(identifier: Char, init: Button.() -> Unit) {
        val button = Button(identifier)
        button.init()
        buttons[button.identifier] = button
    }

    private fun buildInventory(inventoryTitle: String): CustomMenu {

        return object : CustomMenu {
            private val inventory = Bukkit.createInventory(this, rows.size * CELLS, inventoryTitle.mm())

            private val actions = HashMap<Int, (Player) -> Unit>()

            init {
                for ((i, array) in rows.withIndex()) {
                    for ((j, char) in array.withIndex()) {
                        buttons[char]?.let {
                            inventory.setItem(j + CELLS * i, it.icon)
                            it.onClick?.let { callback -> actions[j + CELLS * i] = callback }
                        }
                    }
                }
            }

            override fun handleClick(slot: Int, player: Player) {
                actions[slot]?.invoke(player)
            }

            override fun getInventory(): Inventory = inventory

        }
    }

    companion object {

        fun create(menuTitle: String, init: Menu.() -> Unit): InventoryHolder {
            val menu = Menu()
            menu.init()
            return menu.buildInventory(menuTitle)
        }

    }

    data class Button(
        var identifier: Char,
        var icon: ItemStack = ItemStack(Material.AIR),
        var onClick: ((player: Player) -> Unit)? = null
    )
}