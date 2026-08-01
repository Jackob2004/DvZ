package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.*
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.EndermanWatcher
import org.bukkit.*
import org.bukkit.Vibration.Destination.BlockDestination
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class Enderman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<EndermanWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.ENDERMAN) {
        isAggressive = false
    }

    override val aiZombieEnabled: Boolean = false

    companion object {
        private const val TELEPORT_COOLDOWN = 3

        private const val SCREAM_COOLDOWN = 3

        private val teleportCooldowns = CooldownUtil(TELEPORT_COOLDOWN * 1000L)

        private val screamCooldowns = CooldownUtil(SCREAM_COOLDOWN* 1000L)
    }

    override fun onActivate() {
        super.onActivate()
        startDisguise(ownerId.toPlayer()!!)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    init {
        EndermanListener
    }

    private fun blinkEffect(location: Location) {
        val effectRange = 8.0
        val darknessEffect = PotionEffect(PotionEffectType.DARKNESS, 5 * 20, 0, false, false)

        for (p in location.getNearbyPlayers(effectRange)) {
            if (GameManager.getPlayerTeam(p) != TeamType.DWARF) continue

            p.addPotionEffect(darknessEffect)
        }

        Particle.WITCH.builder()
            .location(location)
            .offset(1.5, 1.5, 1.5)
            .count(10)
            .extra(0.0)
            .receivers(12, true)
            .spawn()
    }

    private fun teleport(endermanPlayer: Player) {
        if (teleportCooldowns.isOnCooldownSafe(endermanPlayer)) {
            teleportCooldowns.displayCooldown(endermanPlayer)
            return
        }

        val teleportRange = 35.0
        val oldLoc = endermanPlayer.eyeLocation

        val result = endermanPlayer.rayTraceBlocks(teleportRange)
        val targetBlock = result?.hitBlock

        if (targetBlock == null) {
            endermanPlayer.sendActionBar("<yellow>Cannot teleport there".mm())
            return
        }

        val destination = targetBlock.location.add(0.0, 1.0, 0.0)
        destination.yaw = oldLoc.yaw
        destination.pitch = oldLoc.pitch

        endermanPlayer.teleport(destination)
        blinkEffect(oldLoc)
        teleportCooldowns.putOnCooldown(endermanPlayer)

        endermanPlayer.playSound(endermanPlayer.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
    }

    private fun getEnemiesInLine(endermanPlayer: Player): Set<Player> {
        val enemies = mutableSetOf<Player>()

        val range = 9
        val step = 3.0
        val distanceVector = endermanPlayer.eyeLocation.direction.normalize().multiply(step)

        var rangeCovered = 0
        val currPoint = endermanPlayer.eyeLocation

        while (rangeCovered < range) {
            currPoint.add(distanceVector)
            rangeCovered += step.toInt()

            for (e in currPoint.getNearbyEntities(step, step, step)) {
                val dwarfEnemy = e as? Player?: continue
                if (GameManager.getPlayerTeam(dwarfEnemy) != TeamType.DWARF) continue
                if (dwarfEnemy.uniqueId == ownerId) continue

                enemies.add(dwarfEnemy)
            }
        }

        return enemies
    }

    private fun scream(endermanPlayer: Player) = endermanPlayer.withCooldown(screamCooldowns) {
        modifyMobDisguise { isAggressive = true }
        var enemies = getEnemiesInLine(endermanPlayer)
        val repetitions = 6
        val period = 15

        val slowness = PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 4, false, false)
        val darkness = PotionEffect(PotionEffectType.DARKNESS, 5 * 20, 0, false, false)

        var counter = repetitions
        sync(period = TimeUnit.TICKS(period.toLong())) {
            if (!this@withCooldown.isOnline) {
                cancel()
                return@sync
            }

            for (e in enemies) {
                e.addPotionEffect(slowness)
                e.addPotionEffect(darkness)
                e.damage(4.0, this@withCooldown)

                Particle.VIBRATION.builder()
                    .location(eyeLocation)
                    .data(Vibration(BlockDestination(e.location), period))
                    .receivers(32, true)
                    .spawn()
            }
            playSound(location, Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f)

            counter--
            if (counter <= 0) {
                cancel()
                this@withCooldown.modifyMobDisguise { isAggressive = false }
            }
            enemies = getEnemiesInLine(endermanPlayer)
        }

        addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, (repetitions * period), 4, false, false))
    }

    object EndermanListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onItemClick(e: PlayerInteractEvent) {
            val player = e.player
            val endermanKit = KitsManager.getKit(player) as? Enderman ?: return
            val rightClicked = e.rightClickItem ?: return

            when (rightClicked.type) {
                Material.COMPASS -> endermanKit.teleport(player)
                Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE -> endermanKit.scream(player)
                else -> Unit
            }

        }
    }
}