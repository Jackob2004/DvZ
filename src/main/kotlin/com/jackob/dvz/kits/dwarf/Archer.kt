package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.launchPlayer
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.withCooldown
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Archer(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero) {

    init {
        ArcherListener
    }

    companion object {
        private val launchCooldowns = CooldownUtil(10 * 1000)
    }

    private fun launchBackwards(player: Player) = player.withCooldown(launchCooldowns) {
        launchPlayer(-1.6, 1.3)
        addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 4 * 20, 0))
        playSound(location, Sound.ENTITY_SLIME_JUMP, 1f, 1f)
    }

    object ArcherListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onBowClick(e: PlayerInteractEvent) {
            val item = e.leftClickItem ?: return
            if (item.type != Material.BOW) return
            val player = e.player
            val kit = KitsManager.getKit(player) as? Archer ?: return

            kit.launchBackwards(player)
        }
    }

}