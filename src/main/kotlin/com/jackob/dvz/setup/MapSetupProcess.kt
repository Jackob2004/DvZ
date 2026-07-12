package com.jackob.dvz.setup

import com.jackob.dvz.DvZ
import com.jackob.dvz.storage.GameMapDraft
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.updateItem
import com.jackob.dvz.util.withPrefix
import com.sk89q.worldedit.IncompleteRegionException
import com.sk89q.worldedit.LocalSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.world.World
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.managers.storage.StorageException
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class MapSetupProcess @Throws(IllegalStateException::class) constructor(
    private val player: Player
) : Listener {

    val processWorldName = player.world.name

    val gameMap: GameMapDraft = GameMapDraft()

    private var selectedShrine = 1

    private var requiredRegions: Array<RequiredRegion>? = null

    private val weWorld: World = BukkitAdapter.adapt(player.world)

    private val weSession: LocalSession = WorldEdit.getInstance().sessionManager.get(BukkitAdapter.adapt(player))

    private val regionManager: RegionManager =
        WorldGuard.getInstance().platform.regionContainer.get(weWorld)
            ?: throw IllegalStateException("Internal error, could not manage regions for the world: $processWorldName")

    init {
        regionManager.setRegions(emptyMap())
        try {
            regionManager.saveChanges()
        } catch (e: StorageException) {
            e.printStackTrace()
            throw IllegalStateException("Internal error, could not reset regions to began setup process")
        }

        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    private fun canGiveConfigTools(): Boolean = with(gameMap) {
        !name.isNullOrBlank() && totalShrines != null && totalShrines!! > 0
    }

    private fun locationsSet(): Boolean = with(gameMap) {
        dwarfSpawn != null &&
                zombieSpawn != null &&
                goldMine != null &&
                sawmill != null &&
                oil != null &&
                shrines.size == totalShrines
    }

    private fun selectNextShrine() {
        selectedShrine++
        if (selectedShrine > gameMap.totalShrines!!) {
            selectedShrine = 1
        }
    }

    private fun generateRequiredRegions() {
        val regions = mutableListOf(
            RequiredRegion("zombie-spawn", false), RequiredRegion("dwarf-spawn", false),
            RequiredRegion("zombie-area", false)
        )

        for (i in 1..gameMap.totalShrines!!) {
            regions.add(RequiredRegion("inner-shrine-$i", false))
            regions.add(RequiredRegion("outer-shrine-$i", false))
        }

        requiredRegions = regions.toTypedArray()
    }

    private fun regionsSet(): Boolean {
        if (requiredRegions == null) return false

        for ((id, _) in requiredRegions) {
            if (!regionManager.hasRegion(id)) return false
        }

        return true
    }

    private fun setRegion(regionId: String, idx: Int) {
        val selection: Region

        try {
            selection = weSession.getSelection(weWorld)
        } catch (ex: IncompleteRegionException) {
            ex.printStackTrace()
            player.sendMessage("<red>Please select a region first!".mm())
            return
        }

        val region = ProtectedCuboidRegion(regionId, selection.minimumPoint, selection.maximumPoint)

        val regex = "-\\d$".toRegex()
        when (regionId.replace(regex, "")) {
            "zombie-spawn" -> configureZombieSpawnRegion(region)
            "dwarf-spawn", "zombie-area", "inner-shrine" -> configureBasicRegionProtection(region)
        }

        regionManager.addRegion(region)
        requiredRegions!![idx].isSet = true
    }

    private fun configureZombieSpawnRegion(region: ProtectedCuboidRegion) = with(region) {
        setFlag(Flags.PVP, StateFlag.State.DENY)
        setFlag(Flags.INVINCIBILITY, StateFlag.State.ALLOW)
    }

    private fun configureBasicRegionProtection(region: ProtectedCuboidRegion) = with(region) {
        setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY)
        setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY)
        setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY)
        setFlag(Flags.TNT, StateFlag.State.DENY)
        setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY)
        setFlag(Flags.OTHER_EXPLOSION, StateFlag.State.DENY)
        setFlag(Flags.GHAST_FIREBALL, StateFlag.State.DENY)
        setFlag(Flags.WATER_FLOW, StateFlag.State.DENY)
        setFlag(Flags.LAVA_FLOW, StateFlag.State.DENY)
        setFlag(Flags.PVP, StateFlag.State.ALLOW)
    }

    fun setBasicInfo(mapName: String, numberOfShrines: Int) {
        gameMap.name = mapName
        gameMap.totalShrines = numberOfShrines
        generateRequiredRegions()
    }

    fun isComplete(): Boolean {
        return canGiveConfigTools() && locationsSet() && regionsSet()
    }

    fun closeProcess() {
        HandlerList.unregisterAll(this)
    }

    fun saveRegions(): Boolean {
        try {
            regionManager.save()
            return true
        } catch (ex: StorageException) {
            ex.printStackTrace()
            player.sendMessage("<red>An unexpected error occurred while saving regions for the map!".mm())
        }

        return false
    }

    fun printRegionsConfig(): Boolean {
        if (requiredRegions == null) return false
        if (!locationsSet()) return false

        val regionsList = Component.text().append(Component::newline)

        for ((idx, region) in requiredRegions!!.withIndex()) {
            val isSetColorIndicator = if (region.isSet) "<green>" else "<white>"
            regionsList.append("<gray> - $isSetColorIndicator${region.id}".mm().clickEvent(ClickEvent.callback {
                setRegion(region.id, idx)
                printRegionsConfig()
            }))
            regionsList.append(Component::newline).append(Component::newline)
        }

        player.sendMessage("<u>Click to set selected cuboid as region<newline>(use WorldEdit wand)".withPrefix().mm())
        player.sendMessage(regionsList.build())

        return true
    }

    fun giveConfigTools(): Boolean {
        if (!canGiveConfigTools()) return false

        val zombieSpawnTool = createItem(Material.ZOMBIE_HEAD) {
            name = "<dark_red><b>Set zombie spawn"
            description = """
                <gray>Usage: <white><b>RIGHT
                <dark_gray>Click to set zombie spawn position. Clicking again will just update.
            """
            enchant(Enchantment.UNBREAKING, 10)
        }

        val dwarfSpawnTool = createItem(Material.PLAYER_HEAD) {
            name = "<dark_green><b>Set dwarf spawn"
            description = """
                <gray>Usage: <white><b>RIGHT
                <dark_gray>Click to set dwarf spawn position. Clicking again will just update.
            """
            enchant(Enchantment.UNBREAKING, 10)
        }

        val shrinesTool = createItem(Material.ENCHANTING_TABLE) {
            name = "<yellow><b>Set shrine position (#$selectedShrine)"
            description = """
                <gray>Scroll shrines: <white><b>LEFT
                <gray>Usage: <white><b>RIGHT
                <dark_gray>Click to either scroll through shrines or set their positions.
                <dark_gray>This positon will also be used as zombie spawn position when the shrine falls.
            """
            enchant(Enchantment.UNBREAKING, 10)
        }

        val goldmineTool = createItem(Material.GOLD_BLOCK) {
            name = "<gold>Set goldmine position"
            description = """
                <gray>Usage: <white><b>RIGHT
                <dark_gray>Click to set goldmine position. Clicking again will just update.
            """
            enchant(Enchantment.UNBREAKING, 10)
        }

        val sawmillTool = createItem(Material.IRON_BARS) {
            name = "<white><b>Set sawmill position"
            description = """
                <gray>Usage: <white><b>RIGHT
                <dark_gray>Click to set sawmill position. Clicking again will just update.
            """
            enchant(Enchantment.UNBREAKING, 10)
        }

        val oilTool = createItem(Material.SPONGE) {
            name = "<dark_purple><b>Set oil position"
            description = """
                <gray>Usage: <white><b>RIGHT
                <dark_gray>Click to set oil position. Clicking again will just update.
            """
            enchant(Enchantment.UNBREAKING, 10)
        }

        player.inventory.clear()
        player.inventory.addItem(zombieSpawnTool, dwarfSpawnTool, shrinesTool, goldmineTool, sawmillTool, oilTool)
        return true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.player != this.player) return
        if (player.world.name != processWorldName) return
        val configTool = event.item ?: return

        val action = event.action
        var message: String? = null
        var sound = Sound.BLOCK_WET_GRASS_PLACE

        when (configTool.type) {
            Material.ZOMBIE_HEAD -> if (action.isRightClick) {
                gameMap.zombieSpawn = player.location
                message = "<gray>Zombie spawn was set"
            }

            Material.PLAYER_HEAD -> if (action.isRightClick) {
                gameMap.dwarfSpawn = player.location
                message = "<gray>Dwarf spawn was set"
            }

            Material.GOLD_BLOCK -> if (action.isRightClick) {
                gameMap.goldMine = player.location
                message = "<gray>Goldmine position was set"
            }

            Material.IRON_BARS -> if (action.isRightClick) {
                gameMap.sawmill = player.location
                message = "<gray>Sawmill position was set"
            }

            Material.SPONGE -> if (action.isRightClick) {
                gameMap.oil = player.location
                message = "<gray>Oil position was set"
            }

            Material.ENCHANTING_TABLE -> {
                if (action.isRightClick) {
                    gameMap.shrines[selectedShrine] = player.location
                    message = "<gray>Shrine (#$selectedShrine) position was set"
                } else if (action.isLeftClick) {
                    selectNextShrine()
                    configTool.updateItem {
                        name = "<yellow><b>Set shrine position (#$selectedShrine)"
                    }
                    sound = Sound.ENTITY_BOAT_PADDLE_WATER
                }
            }

            else -> return
        }

        if (message != null) {
            player.sendMessage(message.withPrefix().mm())
            player.playSound(player.location, sound, 1f, 1f)
        }
    }

    data class RequiredRegion(val id: String, var isSet: Boolean)
}