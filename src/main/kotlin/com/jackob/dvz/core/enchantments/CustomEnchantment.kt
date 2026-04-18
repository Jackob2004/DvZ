package com.jackob.dvz.core.enchantments

import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import org.bukkit.enchantments.Enchantment

/**
 * Represents a base class for defining custom enchantments.
 *
 * Subclasses must provide the [key] used for identification and implement [buildEnchantment]
 * to configure the enchantment's properties (such as description, supported items, and max level).
 *
 */
@Suppress("UnstableApiUsage")
abstract class CustomEnchantment {

    abstract val key: TypedKey<Enchantment>

    abstract fun buildEnchantment(): (EnchantmentRegistryEntry.Builder) -> Unit

    protected fun applyCommonConfig(builder: EnchantmentRegistryEntry.Builder) = with(builder) {
        anvilCost(1)
        weight(1)
        minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
        maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
    }

}