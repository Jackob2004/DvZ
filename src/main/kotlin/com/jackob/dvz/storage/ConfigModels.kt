package com.jackob.dvz.storage

import com.charleskorn.kaml.Yaml
import com.jackob.dvz.DvZ
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.toAttribute
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.potion.PotionEffect
import java.io.File

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
    val amount: Int = 1,
    val name: String,
    val lore: List<String> = emptyList(),
    val enchantments: Map<String, Int> = emptyMap(),
    val attributes: List<AttributeConfig> = emptyList(),

    val basePotionType: String? = "UNCRAFTABLE",
    val potionColor: String? = null,
    val potionEffects: List<PotionConfig> = emptyList(),

    val trimMaterial: String? = null,
    val trimPattern: String? = null,
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

@Serializable
data class Obtainable(
    val rackType: String,
    val pickUpSound: String,
    val item: ItemConfig
)

@Serializable
data class ObtainableRegistry(
    val obtainables: List<Obtainable>
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

    val item = ItemStack(material, amount)
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

    if (meta is ArmorMeta && trimMaterial != null && trimPattern != null) {
        val material = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_MATERIAL)
            .get(NamespacedKey.minecraft(trimMaterial.lowercase()))!!
        val pattern = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN)
            .get(NamespacedKey.minecraft(trimPattern.lowercase()))!!

        val trim = ArmorTrim(material, pattern)

        meta.trim = trim
    }

    item.itemMeta = meta

    return item
}

/**
 * Loads and deserializes a YAML file from the plugin's data folder into [T].
 * If the file doesn't exist, it is copied from the plugin's resources.
 *
 * @param T The target type to deserialize into. Must be [@Serializable].
 * @param fileName The name of the YAML file (e.g. "config.yml").
 * @return The deserialized object, or null if parsing fails.
 */
inline fun <reified T> loadConfig(fileName: String): T? {
    val file = File(DvZ.INSTANCE.dataFolder, fileName)
    if (!file.exists()) {
        DvZ.INSTANCE.saveResource(fileName, false)
    }

    val yamlString = file.readText()

    return try {
        Yaml.default.decodeFromString(serializer<T>(), yamlString)
    } catch (e: Exception) {
        DvZ.INSTANCE.logger.severe("Failed to parse $fileName! Check your syntax: ${e.message}")
        null
    }
}