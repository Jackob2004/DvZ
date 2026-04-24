package com.jackob.dvz.storage

import com.jackob.dvz.DvZ
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import org.bukkit.*
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

private const val MAP_TEMPLATE_SUFFIX = "-template"

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

    private fun copyWorld(source: File, target: File) {
        source.copyRecursively(target, overwrite = true)

        File(target, "uid.dat").delete()
        File(target, "session.lock").delete()
    }

    private fun World.configureWorldSettings(): World {
        setGameRule(GameRules.ADVANCE_TIME, false)
        setGameRule(GameRules.ADVANCE_WEATHER, false)
        setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false)
        setGameRule(GameRules.SHOW_DEATH_MESSAGES, false)
        setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
        setGameRule(GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS, false)

        setGameRule(GameRules.SPAWN_MOBS, false)
        setGameRule(GameRules.SPAWN_MONSTERS, false)
        setGameRule(GameRules.SPAWN_PHANTOMS, false)
        setGameRule(GameRules.SPAWN_PATROLS, false)
        setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false)
        setGameRule(GameRules.SPAWN_WARDENS, false)
        setGameRule(GameRules.RAIDS, false)

        time = 6000
        setStorm(false)
        isThundering = false
        difficulty = Difficulty.NORMAL

        isAutoSave = false

        return this
    }

    @Throws(MapLoadException::class)
    fun copyTemplateRegions(templateName: String, gameMapWorld: World) {
        val templateWorld = Bukkit.getWorld(templateName)
            ?: throw MapLoadException(templateName, "Template world is not loaded!")

        val container = WorldGuard.getInstance().platform.regionContainer

        val templateManager = container.get(BukkitAdapter.adapt(templateWorld))
            ?: throw MapLoadException(templateName, "WorldGuard RegionManager for template missing.")
        val gameManager = container.get(BukkitAdapter.adapt(gameMapWorld))
            ?: throw MapLoadException(gameMapWorld.name, "WorldGuard RegionManager for game map missing.")

        for (templateRegion in templateManager.getRegions().values) {
            if (templateRegion.id == "__global__") {
                val gameGlobal = gameManager.getRegion("__global__")
                gameGlobal?.setFlags(templateRegion.flags)
                continue
            }

            gameManager.addRegion(templateRegion)
        }
    }

    @Throws(MapLoadException::class)
    fun resetMap(templateName: String): World {
        val templateFolder = File(Bukkit.getWorldContainer(), templateName)
        val mapName = templateName.removeSuffix(MAP_TEMPLATE_SUFFIX)
        val mapWorld = File(Bukkit.getWorldContainer(), mapName)

        if (!templateFolder.exists()) {
            throw MapLoadException(templateName, "Template world directory does not exist on disk!")
        }

        Bukkit.getWorld(mapName)?.let { world ->
            world.loadedChunks.forEach { it.unload(false) }
            Bukkit.unloadWorld(world, false)
        }

        if (mapWorld.exists()) {
            mapWorld.deleteRecursively()
        }

        copyWorld(templateFolder, mapWorld)
        val copiedWorld = Bukkit.createWorld(WorldCreator(mapName))?.configureWorldSettings()
            ?: throw MapLoadException(mapName, "Failed to create the copied world!")
        copyTemplateRegions(templateName, copiedWorld)

        return copiedWorld
    }

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
        config.set("$MAPS_PATH.$worldName.goldmine", mapDraft.goldMine!!)
        config.set("$MAPS_PATH.$worldName.sawmill", mapDraft.sawmill!!)
        config.set("$MAPS_PATH.$worldName.oil", mapDraft.oil!!)

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

    fun deleteMap(mapKey: String): Boolean {
        config.set("$MAPS_PATH.$mapKey", null)

        try {
            config.save(file)
        } catch (_: IOException) {
            return false
        }

        return true
    }

    fun getMapKeys(): List<String>? {
        val configSection = config.getConfigurationSection(MAPS_PATH) ?: return null

        return configSection.getKeys(false).toList()
    }

    fun getMapData(templateName: String): GameMap {
        val worldCopy = resetMap(templateName)
        val name = config.getString("$MAPS_PATH.$templateName.name")!!

        val dwarfSpawn = config.getLocation("$MAPS_PATH.$templateName.dwarf-spawn")!!.clone().apply {
            world = worldCopy
        }
        val zombieSpawn = config.getLocation("$MAPS_PATH.$templateName.zombie-spawn")!!.clone().apply {
            world = worldCopy
        }

        val goldmine = config.getLocation("$MAPS_PATH.$templateName.goldmine")!!.clone().apply {
            world = worldCopy
        }
        val sawmill = config.getLocation("$MAPS_PATH.$templateName.sawmill")!!.clone().apply {
            world = worldCopy
        }
        val oil = config.getLocation("$MAPS_PATH.$templateName.oil")!!.clone().apply {
            world = worldCopy
        }

        val shrines = config.getConfigurationSection("$MAPS_PATH.$templateName.shrines")!!.getKeys(false).map {
            config.getLocation("$MAPS_PATH.$templateName.shrines.$it")!!.clone().apply {
                world = worldCopy
            }
        }

        return GameMap(name, dwarfSpawn, zombieSpawn, goldmine, sawmill, oil, shrines)
    }

    fun cleanMapCopies() {
        val serverRoot = Bukkit.getWorldContainer()

        val baseNames = serverRoot.listFiles { file ->
            file.isDirectory && file.name.endsWith(MAP_TEMPLATE_SUFFIX)
        }?.map { it.name.removeSuffix(MAP_TEMPLATE_SUFFIX) } ?: emptyList()

        if (baseNames.isEmpty()) return

        serverRoot.listFiles { file ->
            file.isDirectory && !file.name.endsWith(MAP_TEMPLATE_SUFFIX)
        }?.forEach { folder ->
            val isMapCopy = baseNames.any { baseName -> folder.name.equals(baseName) }

            if (isMapCopy) {
                val success = folder.deleteRecursively()

                if (success) {
                    DvZ.INSTANCE.logger.info("Cleaned up map copy: ${folder.name}")
                } else {
                    DvZ.INSTANCE.logger.warning("Failed to fully delete map copy: ${folder.name}")
                }
            }
        }
    }

    class MapLoadException(mapName: String, message: String) : Exception("[$mapName] $message")
}