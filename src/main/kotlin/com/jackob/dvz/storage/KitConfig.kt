package com.jackob.dvz.storage

import com.charleskorn.kaml.Yaml
import com.jackob.dvz.DvZ
import kotlinx.serialization.Serializable
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
data class KitConfig(
    val potions: List<PotionConfig> = emptyList(),
    val attributes: List<AttributeConfig> = emptyList(),
    val items: List<ItemConfig> = emptyList(),
)

@Serializable
data class KitConfigRegistry(
    val kits: Map<String, KitConfig>
)

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