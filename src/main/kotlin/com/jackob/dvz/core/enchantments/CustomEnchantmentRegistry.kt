package com.jackob.dvz.core.enchantments

import com.jackob.dvz.DvZ
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.Listener

object CustomEnchantmentRegistry {

    val enchantments: List<CustomEnchantment> = listOf(
        RadianceEnchantment()
    )

    val routerMap: Map<String, CustomEnchantment> = enchantments.associateBy {
        it.key.key().asString()
    }

    lateinit var RADIANCE: Enchantment

    fun init() {
        for (e in enchantments) {
            if (e is Listener) {
                DvZ.INSTANCE.server.pluginManager.registerEvents(e, DvZ.INSTANCE)
            }
        }

        val registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
        RADIANCE = registry.get(Key.key("dvz:radiance"))!!
    }

}