package com.jackob.dvz.core.equipment

import com.jackob.dvz.core.objects.DarknessTask
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.sync
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private const val EFFECT_DURATION_IN_SECONDS = 60

class RadianceShroom : CustomItem(), Listener {

    private val protectedBlocks: MutableSet<Location> = HashSet()

    override val item: ItemStack = createItem(Material.SHROOMLIGHT) {
        name = "<b><white>Radiance shroom"
        description = """
              <gray>Placing it will crate protected source of light
              <gray>that will last 60 sec.
              <gray>It can be only destroyed by explosions.
        """
        enchant(Enchantment.UNBREAKING, 10)
        persistentDataContainer.set(DarknessTask.RADIANCE, PersistentDataType.BOOLEAN, true)
    }

    override val type: CustomItemType = CustomItemType.RADIANCE_SHROOM

    private fun spawnProtectedLightBlock(placedBlock: Block, player: Player) {
        val placedBlockLocation = placedBlock.location
        protectedBlocks.add(placedBlockLocation)

        var timeLeft = EFFECT_DURATION_IN_SECONDS
        val displayLoc = placedBlockLocation.clone().add(0.5, 1.5, 0.5)
        val timeDisplay =
            (placedBlockLocation.world.spawnEntity(displayLoc, EntityType.TEXT_DISPLAY) as TextDisplay).apply {
                text("<gray>$timeLeft".mm())
                billboard = Display.Billboard.CENTER
            }

        sync(period = TimeUnit.SECONDS(1)) {
            timeLeft--
            if (timeLeft <= 0 || placedBlock.isEmpty) {
                protectedBlocks.remove(placedBlockLocation)
                timeDisplay.remove()
                this.cancel()
            } else {
                timeDisplay.text("<gray>$timeLeft".mm())
            }
        }

        player.playSound(displayLoc, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 1f)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!isCustomItem(event.itemInHand)) return

        spawnProtectedLightBlock(event.blockPlaced, event.player)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.block.location in protectedBlocks) {
            event.isCancelled = true
        }
    }
}