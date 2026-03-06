package com.jackob.dvz.storage

import com.jackob.dvz.DvZ
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

object MapStorage {

    private const val LOBBY_PATH = "lobby-spawn"
    private const val MAPS_PATH = "maps"

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

    fun saveMap(mapDraft: GameMapDraft): Boolean {
        val worldName = mapDraft.dwarfSpawn!!.world.name

        config.set("$MAPS_PATH.$worldName.name", mapDraft.name!!)
        config.set("$MAPS_PATH.$worldName.dwarf-spawn", mapDraft.dwarfSpawn!!)
        config.set("$MAPS_PATH.$worldName.zombie-spawn", mapDraft.zombieSpawn!!)

        mapDraft.shrines.toSortedMap().forEach {
            config.set("$MAPS_PATH.$worldName.shrines.${it.key}", it.value)
        }

        try {
            config.save(file)
        } catch (_: IOException) {
            return false
        }

        return true
    }

}