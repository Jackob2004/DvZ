package com.jackob.dvz.util

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

fun createItem(material: Material, amount: Int = 1, init: ItemMeta.() -> Unit): ItemStack {
    val item = ItemStack(material, amount)

    val meta = item.itemMeta ?: return item
    meta.apply(init)
    item.itemMeta = meta

    return item
}

@JvmName("createItemGeneric")
inline fun <reified T : ItemMeta> createItem(
    material: Material,
    amount: Int = 1,
    init: T.() -> Unit
): ItemStack {
    val item = ItemStack(material, amount)

    val meta = item.itemMeta as? T ?: return item
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

fun ItemStack.repair(percentage: Int): Boolean {
    require(percentage in 0..100) { "Percentage must be between 0 and 100" }
    val meta = itemMeta as? Damageable ?: return false

    val maxDurability = type.maxDurability
    val repairValue = (10 * 100.0 / maxDurability).toInt()
    val updatedDamage = (meta.damage - repairValue).coerceIn(0, maxDurability.toInt())
    meta.damage = updatedDamage
    itemMeta = meta

    return true
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

val PlayerInteractEvent.leftClickItem: ItemStack?
    get() {
        if (hand != EquipmentSlot.HAND) return null
        if (!action.isLeftClick) return null

        return item?.takeIf { it.type != Material.AIR }
    }
