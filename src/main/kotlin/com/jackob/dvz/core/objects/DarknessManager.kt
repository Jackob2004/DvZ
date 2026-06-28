package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.LightSourceBreakEvent
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.packCoordinates
import com.jackob.dvz.util.sync
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask

class DarknessManager: Listener {

    private var task: BukkitTask? = null

    private val lightSourceBlocks: LongOpenHashSet = LongOpenHashSet()

    companion object {
        private val BLINDNESS_EFFECT = PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 3)
        val RADIANCE = NamespacedKey(DvZ.INSTANCE, "radiance")
    }

    fun unregister(clearInternalState: Boolean = false) {
        HandlerList.unregisterAll(this)
        if (task != null && !task!!.isCancelled) {
            task!!.cancel()
            task = null
        }

        if (clearInternalState) lightSourceBlocks.clear()
    }

    fun register(players: Collection<Player>) {
        check(task == null) { "There can be only one darkness task running!"}
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)

        task = sync(period = TimeUnit.SECONDS(1)) {
            for (player in players) {
                if (GameManager.getPlayerTeam(player) != TeamType.DWARF) continue

                val itemInPlayerHand = player.inventory.itemInMainHand
                if (itemInPlayerHand.persistentDataContainer.has(RADIANCE)) {
                    player.removePotionEffect(PotionEffectType.BLINDNESS)
                    continue
                }

                val lightLevel = player.eyeLocation.block.lightLevel
                if (lightLevel < 5) {
                    player.addPotionEffect(BLINDNESS_EFFECT)
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.itemInHand.persistentDataContainer.has(RADIANCE)) {
            val block = event.blockPlaced
            lightSourceBlocks.add(block.packCoordinates())
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val cordsAsLong: Long = block.packCoordinates()

        if (lightSourceBlocks.contains(cordsAsLong)) {
            lightSourceBlocks.remove(cordsAsLong)
            if (GameManager.getPlayerTeam(event.player) == TeamType.ZOMBIE) {
                Bukkit.getPluginManager().callEvent(LightSourceBreakEvent(event.player))
            }
            event.isDropItems = false
        }
    }
}