package com.jackob.dvz.core.equipment

import com.jackob.dvz.DvZ
import com.jackob.dvz.ui.Menu
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.name
import com.jackob.dvz.util.updateItem
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class Compass(val locations: List<NamedLocation>) : CustomItem(), Listener {

    private val menu = Menu.create("<green>Set compass target") {
        val firstRow = (0..<locations.size).joinToString(separator = "").padEnd(9, '_')
        pattern(firstRow)

        for ((idx, loc) in locations.withIndex()) {
            button(idx.digitToChar()) {
                icon = createItem(loc.icon) {
                    name = loc.name
                }
                onClick = {
                    it.compassTarget = loc.location
                    it.inventory.itemInMainHand.updateItem { name = "<green>Pointing <white>${loc.name}" }
                    it.closeInventory()
                    it.playSound(it.location, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1f, 1f)
                }
            }
        }
    }

    fun registerCompass() {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun unregisterCompass() {
        HandlerList.unregisterAll(this)
    }

    override val item: ItemStack = createItem(Material.COMPASS) {
        name = "<green>Dwarven compass"
        description = """
           Click to open available waypoints
           Use compass to easily reach key map locations
        """
    }

    override val type: CustomItemType = CustomItemType.DWARVEN_COMPASS

    @EventHandler
    fun onItemClick(e: PlayerInteractEvent) {
        if (e.hand != EquipmentSlot.HAND) return

        val item = e.item ?: return
        if (item.type == Material.AIR) return
        if (!isCustomItem(item)) return

        e.player.openInventory(menu.inventory)
        e.isCancelled = true
    }

    data class NamedLocation(val name: String, val icon: Material, val location: Location)
}