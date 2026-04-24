package com.jackob.dvz.core.equipment

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.Team
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.getSphere
import com.jackob.dvz.util.name
import com.jackob.dvz.util.removeItem
import com.jackob.dvz.util.rightClickItem
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class SuperMortar : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.YELLOW_DYE) {
        name = "<b><blue>Super Mortar"
        description = """
           <b><white>[R] <reset>click on a block strengthen with mortal to make it even stronger.
           Blocks affected by super mortal are immune to explosions
           as exposure to them only remove first layer of the super block.
        """
        enchant(Enchantment.UNBREAKING, 10)
    }

    override val type: CustomItemType = CustomItemType.SUPER_MORTAR

    private fun removeExtraLayer(blockList: MutableList<Block>) {
        val iterator = blockList.iterator()

        while (iterator.hasNext()) {
            val b = iterator.next()
            if (b.type == Material.POLISHED_BLACKSTONE_BRICKS) {
                b.type = Material.STONE_BRICKS

                iterator.remove()
            }
        }
    }

    private fun enhanceStrongerBlocks(player: Player, clickedBlockLocation: Location) {
        val strengthenBlocksAround = clickedBlockLocation.getSphere(3, false)
            .map { it.block }
            .filter { block -> block.type == Material.PURPUR_BLOCK }

        for (block in strengthenBlocksAround) {
            block.type = Material.POLISHED_BLACKSTONE_BRICKS
        }

        player.playSound(player.location, Sound.BLOCK_POLISHED_DEEPSLATE_HIT, 1f, 1f)
    }

    @EventHandler
    fun onItemClick(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val item = event.rightClickItem ?: return
        if (!isCustomItem(item)) return

        val player = event.player
        player.removeItem(item, 1)
        enhanceStrongerBlocks(player, block.location)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (GameManager.getPlayerTeam(event.player) != Team.ZOMBIE) return
        val block = event.block
        if (block.type != Material.POLISHED_BLACKSTONE_BRICKS) return

        event.isCancelled = true
        block.type = Material.STONE_BRICKS
    }

    @EventHandler
    fun onEntityExplode(e: EntityExplodeEvent) {
        removeExtraLayer(e.blockList())
    }

    @EventHandler
    fun onBlockExplode(e: BlockExplodeEvent) {
        removeExtraLayer(e.blockList())
    }

}