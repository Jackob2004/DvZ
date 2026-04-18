package com.jackob.dvz.core.enchantments

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.keys.EnchantmentKeys
import io.papermc.paper.registry.keys.ItemTypeKeys
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlotGroup

@Suppress("UnstableApiUsage")
class RadianceEnchantment : CustomEnchantment(), InteractionEnchantment {

    override val key: TypedKey<Enchantment> = EnchantmentKeys.create(Key.key("dvz:radiance"))

    override fun buildEnchantment(): (EnchantmentRegistryEntry.Builder) -> Unit = { b ->
        b.description(Component.text("Radiance"))
            .supportedItems(
                RegistrySet.keySet(
                    RegistryKey.ITEM,
                    ItemTypeKeys.TORCH,
                    ItemTypeKeys.LANTERN,
                    ItemTypeKeys.SOUL_TORCH,
                )
            )
            .maxLevel(3)
            .activeSlots(EquipmentSlotGroup.MAINHAND)
        applyCommonConfig(b)
    }

    override fun handleItemUse(event: PlayerInteractEvent, level: Int) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        event.player.sendMessage("$level")
    }

}