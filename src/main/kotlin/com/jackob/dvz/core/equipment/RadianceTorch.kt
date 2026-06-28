package com.jackob.dvz.core.equipment

import com.jackob.dvz.core.objects.DarknessManager
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.name
import com.jackob.dvz.util.sync
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.exp

private const val EFFECT_DURATION_IN_SECONDS = 15
class RadianceTorch : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.COPPER_TORCH) {
        name = "<b><white>Radiance torch"
        description = """
              <gray>Spawns light blast effect that lasts 15 sec.
              <gray>It is truly understandably source of light.
        """
        enchant(Enchantment.UNBREAKING, 10)
        persistentDataContainer.set(DarknessManager.RADIANCE, PersistentDataType.BOOLEAN, true)
    }

    override val type: CustomItemType = CustomItemType.RADIANCE_TORCH

    private fun playLightBlastEffect(placedBlock: Block, player: Player) {
        placedBlock.type = Material.LIGHT

        val durationInTicks = EFFECT_DURATION_IN_SECONDS * 10
        var timeLeft = durationInTicks

        val effectLoc = placedBlock.location
        val particleEffect = Particle.END_ROD.builder()
            .location(effectLoc.add(0.0, 2.0, 0.0))
            .count(1)
            .receivers(8, true)
            .offset(0.0, 0.0, 0.0)
            .extra(0.1)

        val initialStrength = 30
        val decayRate = 5.0
        var elapsed = 0.0

        sync(period = TimeUnit.TICKS(2)) {
            timeLeft--
            elapsed++
            if (timeLeft <= 0) {
                placedBlock.type = Material.AIR
                this.cancel()
            } else {
                val time = elapsed / durationInTicks
                // exponential decay
                val effectIntensity = (initialStrength * exp(-decayRate * time)).toInt()
                particleEffect.count(effectIntensity)
                particleEffect.spawn()
            }
        }

        player.playSound(effectLoc, Sound.BLOCK_LAVA_EXTINGUISH, 1f, 1f)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.blockReplacedState.type == Material.LIGHT) {
            event.isCancelled = true
            return
        }

        if(isCustomItem(event.itemInHand)) {
            playLightBlastEffect(event.block, event.player)
        }
    }
}