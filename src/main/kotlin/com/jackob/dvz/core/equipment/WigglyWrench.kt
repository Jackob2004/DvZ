package com.jackob.dvz.core.equipment

import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.name
import com.jackob.dvz.util.removeItem
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

class WigglyWrench : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.RESIN_BRICK) {
        name = "<b><gray>Wiggly wrench"
        description = """
           <b><white>[R] <reset>click to repair your amor 
        """
        enchant(Enchantment.UNBREAKING, 10)
    }

    override val type: CustomItemType = CustomItemType.WIGGLY_WRENCH

    private fun repairArmor(player: Player) {
        for (item in player.inventory.armorContents) {
            val damageable = item?.itemMeta as? Damageable ?: continue
            damageable.resetDamage()
            item.itemMeta = damageable
        }

        player.playSound(player.location, Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f)
    }

    @EventHandler
    fun onItemClick(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (!event.action.isRightClick) return

        val item = event.item ?: return
        if (item.type == Material.AIR) return
        if (!isCustomItem(item)) return

        val player = event.player
        player.removeItem(item, 1)
        repairArmor(player)
    }
}