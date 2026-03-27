package com.jackob.dvz.kits

import com.jackob.dvz.storage.getKitConfigs
import com.jackob.dvz.storage.toAttributeModifier
import com.jackob.dvz.storage.toItemStack
import com.jackob.dvz.storage.toPotionEffect
import com.jackob.dvz.util.toAttribute
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

object KitConfigsCache {

    private val kitsCache = getKitConfigs()!!.kits

    fun retrieveKitPotions(kitName: String) : List<PotionEffect> {
        return kitsCache[kitName]!!.potions.map { it.toPotionEffect() }
    }

    fun retrieveAttributes(kitName: String) : List<Pair<Attribute, AttributeModifier>> {
        return kitsCache[kitName]!!.attributes.map { Pair(it.type.toAttribute(), it.toAttributeModifier()) }
    }

    fun retrieveKitItems(kitName: String) : List<ItemStack> {
        return kitsCache[kitName]!!.items.map { it.toItemStack() }
    }

    fun retrieveActivationMessages(kitName: String) : Pair<String, Boolean> {
        val serialized = kitsCache[kitName]!!.activateMessage
        return Pair(serialized.message, serialized.broadcast)
    }
}