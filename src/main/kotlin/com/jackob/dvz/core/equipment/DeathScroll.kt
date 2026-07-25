package com.jackob.dvz.core.equipment

import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.removeItem
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class DeathScroll : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.SKULL_BANNER_PATTERN) {
        name = "<white><i>Death Scroll"
        description = """
            Use to commit suicide  
            <green>[Right] <white>- click to use
        """
        enchant(Enchantment.UNBREAKING, 10)
    }

    override val type: CustomItemType = CustomItemType.DEATH_SCROLL

    private val deathTasks = HashMap<UUID, BukkitTask>()

    private fun commitSuicide(player: Player) {
        val playerId = player.uniqueId
        var countdown = 3

        deathTasks[playerId] = sync(period = TimeUnit.SECONDS(1)) {
            player.sendActionBar("Dead in: <gray>${countdown}s".mm())

            countdown--
            if (countdown <= 0) {
                cancel()
                deathTasks.remove(playerId)
                player.health = 0.0
                player.sendActionBar("".mm())
            }
        }
    }

    @EventHandler
    fun onScrollClick(event: PlayerInteractEvent) {
        val item = event.rightClickItem ?: return
        if (!isCustomItem(item)) return

        val player = event.player
        player.removeItem(item, 1)
        commitSuicide(player)
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val playerId = event.player.uniqueId
        if (!deathTasks.containsKey(playerId)) return

        deathTasks[playerId]!!.cancel()
        deathTasks.remove(playerId)
    }
}