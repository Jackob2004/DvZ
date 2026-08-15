package com.jackob.dvz.core.equipment

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.PortalTeleportEvent
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.removeItem
import com.jackob.dvz.util.rightClickItem
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class PortalScroll: CustomItem(), Listener {
    override val item: ItemStack = createItem(PORTAL_SCROLL_TYPE) {
        name = "<dark_purple>Portal Scroll"
        description = """
           Can be used to teleport to the active portal 
           <green>[Right] <white>- click to use
        """
    }

    override val type: CustomItemType = CustomItemType.PORTAL_SCROLL

    companion object {
        val PORTAL_SCROLL_TYPE = Material.FLOWER_BANNER_PATTERN
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPortalScrollUse(event: PlayerInteractEvent) {
        val item = event.rightClickItem ?: return
        if (!isCustomItem(item)) return
        val player = event.player
        if (GameManager.getPlayerTeam(player) != TeamType.ZOMBIE) return

        val portalEvent = PortalTeleportEvent(player)
        portalEvent.callEvent()

        if (portalEvent.isCancelled) {
            player.sendActionBar("<yellow>There is no active portal!".mm())
        } else {
            player.removeItem(item, 1)
            player.sendActionBar("<green>Teleporting...".mm())
        }

        event.isCancelled = true
    }
}