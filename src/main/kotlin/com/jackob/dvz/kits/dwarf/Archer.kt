package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.launchPlayer
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.withCooldown
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Arrow
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Archer(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero) {

    companion object {
        private val launchCooldowns = CooldownUtil(10 * 1000)
    }

    init {
        ArcherListener
    }

    private fun launchBackwards(player: Player) = player.withCooldown(launchCooldowns) {
        launchPlayer(-1.6, 1.3)
        addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 4 * 20, 0))
        playSound(location, Sound.ENTITY_SLIME_JUMP, 1f, 1f)
    }

    private fun handleHeadshot(shooter: Player, arrow: Projectile, victim: LivingEntity) {
        val distance = arrow.location.distanceSquared(victim.eyeLocation)
        if (distance <= 0.2) {
            victim.damage(20.0, shooter)
            shooter.playSound(shooter.location, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f)
        }
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

        @EventHandler
        fun onArrowHit(e: ProjectileHitEvent) {
            val arrow = e.entity as? Arrow ?: return
            val victim = e.hitEntity as? LivingEntity ?: return
            if (victim is Player && GameManager.getPlayerTeam(victim) == TeamType.DWARF) return
            val shooter = e.entity.shooter as? Player ?: return
            val kit = KitsManager.getKit(shooter) as? Archer ?: return

            kit.handleHeadshot(shooter, arrow, victim)
        }
    }

}