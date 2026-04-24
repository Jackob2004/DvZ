package com.jackob.dvz.util

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

fun createItem(material: Material, amount: Int = 1, init: ItemMeta.() -> Unit): ItemStack {
    val item = ItemStack(material, amount)

    val meta = item.itemMeta ?: return item
    meta.apply(init)
    item.itemMeta = meta

    return item
}

fun ItemStack.updateItem(init: ItemMeta.() -> Unit) {
    val meta = itemMeta
    meta.apply(init)
    this.itemMeta = meta
}

var ItemMeta.name: String
    get() {
        val currentName = displayName() ?: return ""
        return MiniMessage.miniMessage().serialize(currentName)
    }
    set(value) {
        displayName(value.mm())
    }

var ItemMeta.description: String
    get() {
        val currentLore = lore() ?: return ""
        return currentLore.joinToString("\n") { MiniMessage.miniMessage().serialize(it) }
    }
    set(value) {
        lore(value.trimIndent().lines().map { it.mm() })
    }

fun ItemMeta.enchant(enchantment: Enchantment, level: Int) {
    addEnchant(enchantment, level, true)
}

/**
 * Returns the item in the main hand only if the player performed a right-click.
 * Returns null if they held nothing.
 */
val PlayerInteractEvent.rightClickItem: ItemStack?
    get() {
        if (hand != EquipmentSlot.HAND) return null
        if (!action.isRightClick) return null

        return item?.takeIf { it.type != Material.AIR }
    }