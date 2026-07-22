package com.jackob.dvz.core.equipment

import com.jackob.dvz.DvZ
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.name
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

private const val FUSE_BURN_TIME = 3

class ExplosiveCharge(
    val onIgnitionStart: (Player) -> Unit,
    val onIgnitionStop: (Player) -> Unit,
    val onExplosion: (Player) -> Unit,
    var explosionPower: Float,
    val owner: UUID
) : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.GUNPOWDER) {
        name = "<green>Explosive Charge"

        description = """
            Start the ignition to detonate yourself, fuse burn time is <gray>${FUSE_BURN_TIME}3s
            <green>[Right] <white>- click to start ignition
            <green>[Left] <white>- click to stop ignition
        """
    }

    override val type: CustomItemType = CustomItemType.EXPLOSIVE_CHARGE

    private var explosionTask: BukkitTask? = null

    override fun onReceive(player: Player) {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    override fun onLose(player: Player) {
        HandlerList.unregisterAll(this)
    }

    private fun explode(player: Player, world: World) {
        val location = player.location.add(0.0, 1.0, 0.0)
        world.createExplosion(location, explosionPower, false, true, player)

        onExplosion(player)
        player.health = 0.0
    }

    private fun startIgnition(player: Player) {
        if (explosionTask != null) return

        val timeToExplosion = TimeUnit.SECONDS(FUSE_BURN_TIME.toLong())
        val world = player.world

        var timer = timeToExplosion
        explosionTask = sync(period = TimeUnit.TICKS(1)) {
            player.exp = (timer * 100 / timeToExplosion / 100f).coerceIn(0f, 1f)

            timer--
            if (timer <= 0) {
                cancel()
                explosionTask = null
                explode(player, world)
            }
        }

        onIgnitionStart(player)
        world.playSound(player.location, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)
    }

    private fun stopIgnition(player: Player) {
        val task = explosionTask
        if (task != null) {
            task.cancel()
            explosionTask = null

            onIgnitionStop(player)
            player.exp = 0f
            player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f)
        }
    }

    @EventHandler()
    fun onExplosiveUse(event: PlayerInteractEvent) {
        val player = event.player
        if (player.uniqueId != owner) return

        val rightClickedItem = event.rightClickItem
        val leftClickedItem = event.leftClickItem

        if (rightClickedItem != null && isCustomItem(rightClickedItem)) {
            startIgnition(player)
        } else if (leftClickedItem != null && isCustomItem(leftClickedItem)) {
            stopIgnition(player)
        }

        event.isCancelled = true
    }

}