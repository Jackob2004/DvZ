package com.jackob.dvz.setup

import com.jackob.dvz.DvZ
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.ui.Menu
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.withPrefix
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object MapSetupManager : Listener {

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    private val processes: MutableMap<Player, MapSetupProcess> = HashMap()

    private fun isBeingConfigured(player: Player): Boolean {
        return player.world.name in processes.values.map { it.processWorldName }
    }

    /**
     * @return false if a new process could not register due to exception
     */
    private fun registerProcess(player: Player): Boolean {
        try {
            if (!processes.containsKey(player)) {
                val process = MapSetupProcess(player)
                processes[player] = process
            }
            return true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

        return false
    }

    private fun unregisterProcess(player: Player) {
        processes[player]?.let {
            it.closeProcess()
            processes.remove(player)
        }
    }

    private fun finishProcess(player: Player, process: MapSetupProcess): Boolean {
        if (process.isComplete() && process.saveRegions() && MapStorage.saveMap(process.gameMap)) {
            unregisterProcess(player)
            return true
        }

        return false
    }

    fun getProcess(player: Player): MapSetupProcess? {
        return processes[player]
    }

    fun openSetupMenu(player: Player) {
        if (!processes.containsKey(player) && isBeingConfigured(player)) {
            player.sendMessage("<yellow>Someone else is configuring this map right now!!!".withPrefix().mm())
            return
        }

        if (!registerProcess(player)) return

        val menu = Menu.create("<b><gray>Map setup menu") {
            val process = processes[player]!!
            pattern(
                "EFFFFFFFF",
                "F_P_T_R_F",
                "CFFFFFFFA"
            )

            button('E') {
                icon = createItem(Material.RED_STAINED_GLASS_PANE) {
                    name = "<red><i>Exit menu"
                    enchant(Enchantment.UNBREAKING, 10)
                }
                onClick = {
                    it.closeInventory()
                }
            }

            button('P') {
                icon = createItem(Material.PALE_OAK_HANGING_SIGN) {
                    name = "<gray><i>Set map name and number of shrines"
                    enchant(Enchantment.UNBREAKING, 10)
                }
                onClick = {
                    MapFormsManager.getForm(FormType.SETUP_FORM).openForm(it)
                }
            }

            button('T') {
                icon = createItem(Material.COPPER_SHOVEL) {
                    name = "<gray><i>Get config tools"
                    description = """
                        Don't forget to set basic information first
                    """
                    enchant(Enchantment.UNBREAKING, 10)
                }
                onClick = {
                    if (process.giveConfigTools()) {
                        it.sendMessage(
                            "<gray>You were given map setup tools. Come back to the menu when you are done.".withPrefix()
                                .mm()
                        )
                    } else {
                        it.sendMessage("<red>You need to fill up basic information first!".withPrefix().mm())
                    }
                }
            }

            button('R') {
                icon = createItem(Material.BARRIER) {
                    name = "<gray><i>Print regions to set"
                    description = """
                        Don't forget to set locations first
                    """
                    enchant(Enchantment.UNBREAKING, 10)
                }
                onClick = {
                    it.closeInventory()
                    if (!process.printRegionsConfig()) {
                        it.sendMessage("<red>You need to set locations first!".withPrefix().mm())
                    }
                }
            }

            button('C') {
                icon = createItem(Material.YELLOW_STAINED_GLASS_PANE) {
                    name = "<yellow><i>Cancel setup process"
                    enchant(Enchantment.UNBREAKING, 10)
                }
                onClick = {
                    unregisterProcess(it)
                    it.closeInventory()
                    it.sendMessage("<gray>Setup process was cancelled".withPrefix().mm())
                    it.inventory.clear()
                }
            }

            button('A') {
                icon = createItem(Material.GREEN_STAINED_GLASS_PANE) {
                    name = "<green><i>Confirm setup process"
                    enchant(Enchantment.UNBREAKING, 10)
                }
                onClick = {
                    if (finishProcess(it, process)) {
                        it.sendMessage("<green>Process finished successfully!".withPrefix().mm())
                        it.closeInventory()
                        it.inventory.clear()
                    } else {
                        it.sendMessage("<red>Process couldn't finish!".withPrefix().mm())
                    }
                }
            }

            button('F') {
                icon = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE) {
                    name = "<u>Info"
                    description = """
                        <gray>Start with basic info form first then proceed with tools 
                        <gray>setting up essential locations.
                    """
                }
            }

            button('_') {}
        }
        player.openInventory(menu.inventory)
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        unregisterProcess(event.player)
    }
}