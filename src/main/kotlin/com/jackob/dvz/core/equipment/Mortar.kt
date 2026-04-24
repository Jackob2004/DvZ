package com.jackob.dvz.core.equipment

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.Team
import com.jackob.dvz.util.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class Mortar : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.PINK_DYE) {
        name = "<b><gray>Mortar"
        description = """
           <b><white>[R] <reset>click on a block to strengthen walls around you
        """
        enchant(Enchantment.UNBREAKING, 10)
    }

    override val type: CustomItemType = CustomItemType.MORTAR


    @Suppress("UnstableApiUsage")
    private fun strengthenWalls(player: Player, clickedBlockLocation: Location) {
        val blocksAround = clickedBlockLocation.getSphere(3, false)
            .map { it.block }
            .filter { block ->
                val blockType = block.type
                Tag.STONE_BRICKS.isTagged(blockType) || blockType == Material.COBBLESTONE || blockType == Material.STONE
            }
        val pluginManager = Bukkit.getPluginManager()

        for (block in blocksAround) {
            val blockBreakSimulate = BlockBreakEvent(block, player)
            pluginManager.callEvent(blockBreakSimulate)

            if (!blockBreakSimulate.isCancelled) {
                block.type = Material.PURPUR_BLOCK
            }
        }

        player.playSound(player.location, Sound.BLOCK_RESIN_BRICKS_HIT, 1f, 1f)
    }

    @EventHandler
    fun onItemClick(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val item = event.rightClickItem ?: return
        if (!isCustomItem(item)) return

        val player = event.player
        player.removeItem(item, 1)
        strengthenWalls(player, block.location)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (GameManager.getPlayerTeam(event.player) != Team.ZOMBIE) return
        val block = event.block
        if (block.type != Material.PURPUR_BLOCK) return

        event.isCancelled = true
        block.type = Material.STONE_BRICKS
    }
}