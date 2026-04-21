package com.jackob.dvz.core.enchantments

import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.mm
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import io.papermc.paper.registry.keys.EnchantmentKeys
import io.papermc.paper.registry.keys.ItemTypeKeys
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.TextDisplay
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlotGroup
import com.jackob.dvz.util.sync
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import kotlin.math.exp

@Suppress("UnstableApiUsage")
class RadianceEnchantment : CustomEnchantment(), PlacementEnchantment, Listener {

    private val protectedBlocks: MutableSet<Location> = HashSet()

    override val key: TypedKey<Enchantment> = EnchantmentKeys.create(Key.key("dvz:radiance"))

    override fun buildEnchantment(): (EnchantmentRegistryEntry.Builder) -> Unit = { b ->
        b.description(Component.text("Radiance"))
            .supportedItems(
                RegistrySet.keySet(
                    RegistryKey.ITEM,
                    ItemTypeKeys.TORCH,
                    ItemTypeKeys.SHROOMLIGHT,
                    ItemTypeKeys.SOUL_TORCH,
                )
            )
            .maxLevel(3)
            .activeSlots(EquipmentSlotGroup.MAINHAND)
        applyCommonConfig(b)
    }

    fun spawnProtectedLightBlock(placedBlockLocation: Location, player: Player, durationInSeconds: Int) {
        protectedBlocks.add(placedBlockLocation)

        var timeLeft = durationInSeconds
        val displayLoc = placedBlockLocation.clone().add(0.5, 1.5, 0.5)
        val timeDisplay =
            (placedBlockLocation.world.spawnEntity(displayLoc, EntityType.TEXT_DISPLAY) as TextDisplay).apply {
                text("<gray>$timeLeft".mm())
                billboard = Display.Billboard.CENTER
            }

        sync(period = TimeUnit.SECONDS(1)) {
            timeLeft--
            if (timeLeft <= 0) {
                protectedBlocks.remove(placedBlockLocation)
                timeDisplay.remove()
                this.cancel()
            } else {
                timeDisplay.text("<gray>$timeLeft".mm())
            }
        }

        player.playSound(displayLoc, Sound.BLOCK_CAMPFIRE_CRACKLE, 1f, 1f)
    }

    fun playLightBlastEffect(placedBlock: Block, player: Player, durationInSeconds: Int) {
        placedBlock.type = Material.LIGHT

        val durationInTicks = durationInSeconds * 10
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

    override fun handleBlockPlace(event: BlockPlaceEvent, level: Int) {
        val placedBlock = event.blockPlaced
        val placedBlockLocation = placedBlock.location
        val player = event.player

        placedBlock.drops.clear()

        when (level) {
            3 -> playLightBlastEffect(placedBlock, player, 15)
            2 -> spawnProtectedLightBlock(placedBlockLocation, player, 60)
            1 -> player.playSound(placedBlockLocation, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f)
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (protectedBlocks.contains(event.block.location)) {
            event.isCancelled = true
        }
    }
}