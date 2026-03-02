package com.jackob.dvz.util

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

fun createItem(material: Material, init: ItemMeta.() -> Unit): ItemStack {
    val item = ItemStack(material)

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