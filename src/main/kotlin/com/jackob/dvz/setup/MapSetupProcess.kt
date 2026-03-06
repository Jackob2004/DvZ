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
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class MapSetupProcess(private val player: Player) : Listener {

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    val processWorldName = player.world.name

    val gameMap: GameMapDraft = GameMapDraft()

    private var selectedShrine = 1

    private fun canGiveConfigTools(): Boolean = with(gameMap) {
        !name.isNullOrBlank() && totalShrines != null && totalShrines!! > 0
    }

    private fun selectNextShrine() {
        selectedShrine++
        if (selectedShrine > gameMap.totalShrines!!) {
            selectedShrine = 1
        }
    }

    fun isComplete(): Boolean = with(gameMap) {
        canGiveConfigTools() &&
                dwarfSpawn != null &&
                zombieSpawn != null &&
                shrines.size == totalShrines
    }

    fun closeProcess() {
        HandlerList.unregisterAll(this)
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

        player.inventory.clear()
        player.inventory.addItem(zombieSpawnTool, dwarfSpawnTool, shrinesTool)
        return true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.player != this.player) return
        if (player.world.name != processWorldName) return
        val configTool = event.item ?: return

        var message: String? = null
        var sound = Sound.BLOCK_WET_GRASS_PLACE

        when (configTool.type) {
            Material.ZOMBIE_HEAD -> if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                gameMap.zombieSpawn = player.location
                message = "<gray>Zombie spawn was set"
            }

            Material.PLAYER_HEAD -> if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                gameMap.dwarfSpawn = player.location
                message = "<gray>Dwarf spawn was set"
            }

            Material.ENCHANTING_TABLE -> {
                if (event.action == Action.RIGHT_CLICK_BLOCK || event.action == Action.RIGHT_CLICK_AIR) {
                    gameMap.shrines[selectedShrine] = player.location
                    message = "<gray>Shrine (#$selectedShrine) position was set"
                } else if (event.action == Action.LEFT_CLICK_BLOCK || event.action == Action.LEFT_CLICK_AIR) {
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
}