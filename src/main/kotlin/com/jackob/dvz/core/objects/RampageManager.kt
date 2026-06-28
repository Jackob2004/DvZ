package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.events.ZombieDeathEvent
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class RampageManager : Listener {

    private val activeRampageMap = CooldownUtil(3000)

    private val visualEffectRecipients: MutableList<UUID> = ArrayList()

    private var visualEffectsTask: BukkitTask? = null

    companion object {
        private const val BASE_RAMPAGE = 20.0
    }

    fun register() {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        startVisualEffect()
    }

    fun unregister() {
        HandlerList.unregisterAll(this)
        visualEffectsTask?.cancel()
        visualEffectsTask = null
    }

    private fun startVisualEffect(): BukkitTask {
        check(visualEffectsTask == null) { "Visual effect task is already running!" }

        val particleEffect = Particle.GLOW.builder()
            .count(5)
            .offset(0.5, 0.5, 0.5)

        return sync(period = TimeUnit.SECONDS(1)) {
            for (i in 0 until visualEffectRecipients.size) {
                val player = visualEffectRecipients[i].toPlayer() ?: continue
                if (!activeRampageMap.isOnCooldownSafe(player)) continue

                particleEffect.location(player.location.add(0.0, 1.0, 0.0))
                    .receivers(12, true)
                    .spawn()
            }
        }
    }

    @EventHandler
    fun onZombieDeath(e: ZombieDeathEvent) {
        val dwarf = e.killer
        if (!activeRampageMap.wasOnCooldown(dwarf)) {
            visualEffectRecipients.add(dwarf.uniqueId)
        }

        activeRampageMap.putOnCooldown(dwarf)
    }

    @EventHandler
    fun onDwarfDealDamage(e: EntityDamageByEntityEvent) {
        val player = e.damager as? Player ?: return
        if (!activeRampageMap.isOnCooldownSafe(player)) return
        if (e.entity is Player && KitsManager.getKit(e.entity as Player)?.kitAttributes?.rampageImmune == true) return
        val rampageModifier = KitsManager.getKit(player)?.kitAttributes?.rampage ?: return

        e.damage = BASE_RAMPAGE * rampageModifier
    }

}