package com.jackob.dvz.storage

import com.charleskorn.kaml.Yaml
import com.jackob.dvz.DvZ
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.toAttribute
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.Serializable
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import java.io.File

const val FILE_NAME = "kits_config.yml"

@Serializable
data class PotionConfig(
    val type: String,
    val durationSeconds: Int,
    val amplifier: Int = 0
)

@Serializable
data class AttributeConfig(
    val type: String,
    val amount: Double,
    val operation: String = "ADD_NUMBER",
    val slot: String? = null
)

@Serializable
data class ItemConfig(
    val material: String,
    val name: String,
    val lore: List<String> = emptyList(),
    val enchantments: Map<String, Int> = emptyMap(),
    val attributes: List<AttributeConfig> = emptyList(),

    val basePotionType: String? = null,
    val potionColor: String? = null,
    val potionEffects: List<PotionConfig> = emptyList()
)

@Serializable
data class ActivationMessage(
    val message: String,
    val broadcast: Boolean
)

@Serializable
data class KitConfig(
    val potions: List<PotionConfig> = emptyList(),
    val attributes: List<AttributeConfig> = emptyList(),
    val items: List<ItemConfig> = emptyList(),
    val activateMessage: ActivationMessage
)

@Serializable
data class KitConfigRegistry(
    val kits: Map<String, KitConfig>
)

fun PotionConfig.toPotionEffect(): PotionEffect {
    val type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(type.lowercase()))!!

    return PotionEffect(type, durationSeconds * 20, amplifier)
}

fun AttributeConfig.toAttributeModifier(): AttributeModifier {
    val operation = AttributeModifier.Operation.valueOf(this.operation)
    val slotGroup = when (slot?.uppercase()) {
        "HAND" -> EquipmentSlotGroup.HAND
        "HEAD" -> EquipmentSlotGroup.HEAD
        "CHEST" -> EquipmentSlotGroup.CHEST
        "LEGS" -> EquipmentSlotGroup.LEGS
        "FEET" -> EquipmentSlotGroup.FEET
        "ARMOR" -> EquipmentSlotGroup.ARMOR
        else -> EquipmentSlotGroup.ANY
    }

    return AttributeModifier(NamespacedKey(DvZ.INSTANCE, "custom$type"), amount, operation, slotGroup)
}

fun ItemConfig.toItemStack(): ItemStack {
    val material = Material.matchMaterial(material)!!
    val name = name.mm()
    val description = lore.map(String::mm)

    val item = ItemStack(material)
    val meta = item.itemMeta
    meta.displayName(name)
    meta.lore(description)

    enchantments.forEach { (key, level) ->
        val enchant = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
            .get(NamespacedKey.minecraft(key.lowercase()))!!
        meta.addEnchant(enchant, level, true)
    }

    attributes.forEach { attrConfig ->
        meta.addAttributeModifier(attrConfig.type.toAttribute(), attrConfig.toAttributeModifier())
    }

    if (meta is PotionMeta) {
        meta.basePotionType = Registry.POTION.get(NamespacedKey.minecraft(basePotionType!!.lowercase()))

        val colorInt = potionColor!!.removePrefix("#").toIntOrNull(16)
        if (colorInt != null) meta.color = Color.fromRGB(colorInt)

        potionEffects.forEach { pConfig ->
            meta.addCustomEffect(pConfig.toPotionEffect(), true)
        }
    }

    item.itemMeta = meta

    return item
}

fun getKitConfigs(): KitConfigRegistry? {
    val file = File(DvZ.INSTANCE.dataFolder, FILE_NAME)
    if (!file.exists()) {
        DvZ.INSTANCE.saveResource(FILE_NAME, false)
    }

    val yamlString = file.readText()

    try {
        return Yaml.default.decodeFromString(KitConfigRegistry.serializer(), yamlString)
    } catch (e: Exception) {
        DvZ.INSTANCE.logger.severe("Failed to parse kits.yml! Check your syntax: ${e.message}")
        return null
    }
}