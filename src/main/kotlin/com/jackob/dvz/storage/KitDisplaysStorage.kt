package com.jackob.dvz.storage

import com.jackob.dvz.DvZ
import com.jackob.dvz.util.mm
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object KitDisplaysStorage {

    private val file = File(DvZ.INSTANCE.dataFolder, "kit_displays.yml").apply {
        if (!exists()) {
            DvZ.INSTANCE.saveResource("kit_displays.yml", false)
        }
    }

    private val config = YamlConfiguration.loadConfiguration(file)

    fun getKitDisplayData(identifier: String): KitDisplay {
        if (!config.contains(identifier)) {
            return KitDisplay()
        }

        val icon = Material.getMaterial(config.getString("$identifier.icon")!!)!!
        val name = config.getString("$identifier.name")!!
        val description = config.getStringList("$identifier.description")

        return KitDisplay(icon, name, description)
    }
}