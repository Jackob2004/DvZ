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
import org.bukkit.Sound
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

@Suppress("UnstableApiUsage")
class RadianceEnchantment : CustomEnchantment(), PlacementEnchantment, Listener{

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

    fun handleSecondLevel(placedBlockLocation : Location, player: Player) {
        protectedBlocks.add(placedBlockLocation)

        var timeLeft = 60
        val displayLoc = placedBlockLocation.clone().add(0.5, 1.5, 0.5)
        val timeDisplay = (placedBlockLocation.world.spawnEntity(displayLoc, EntityType.TEXT_DISPLAY) as TextDisplay).apply {
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

    override fun handleBlockPlace(event: BlockPlaceEvent, level: Int) {
        val placedBlock = event.blockPlaced
        val placedBlockLocation = placedBlock.location
        val player = event.player

        placedBlock.drops.clear()

        when (level) {
            3 -> TODO("Implement handling 3th level")
            2 -> handleSecondLevel(placedBlockLocation, player)
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