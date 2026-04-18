package com.jackob.dvz.core.enchantments

import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.enchantments.Enchantment

object CustomEnchantmentRegistry {

    val enchantments: List<CustomEnchantment> = listOf(
        RadianceEnchantment()
    )

    val routerMap: Map<String, CustomEnchantment> = enchantments.associateBy {
        it.key.key().asString()
    }

    lateinit var RADIANCE: Enchantment

    fun init() {
        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
        RADIANCE = registry.get(Key.key("dvz:radiance"))!!
    }

}