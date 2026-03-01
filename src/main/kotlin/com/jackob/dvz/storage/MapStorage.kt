package com.jackob.dvz.storage

import com.jackob.dvz.DvZ
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

object MapStorage {

    private const val LOBBY_PATH = "lobby-spawn"

    init {
        DvZ.INSTANCE.dataFolder.mkdir()
    }

    private val file = File(DvZ.INSTANCE.dataFolder, "maps.yml").apply {
        if (!exists()) {
            createNewFile()
        }
    }

    private val config = YamlConfiguration.loadConfiguration(file)

    val LOBBY_SPAWN: Location? = config.getLocation(LOBBY_PATH)

    fun saveLobby(lobbyLocation: Location): Boolean {
        config.set(LOBBY_PATH, lobbyLocation)
        try {
            config.save(file)
        } catch (_: IOException) {
            return false
        }

        return true
    }

}