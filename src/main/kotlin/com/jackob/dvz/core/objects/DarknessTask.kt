package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.sync
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask

class DarknessTask {

    private var task: BukkitTask? = null

    companion object {
        private val BLINDNESS_EFFECT = PotionEffect(PotionEffectType.BLINDNESS, 20 * 2, 3)
        val RADIANCE = NamespacedKey(DvZ.INSTANCE, "radiance")
    }

    fun stopTask() {
        if (task != null && !task!!.isCancelled) {
            task!!.cancel()
            task = null
        }
    }

    fun startTask(players: Collection<Player>) {
        check(task == null) { "There can be only one darkness task running!"}

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
}