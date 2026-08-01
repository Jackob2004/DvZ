package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.EndermanWatcher
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Enderman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<EndermanWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.ENDERMAN) { }

    override val aiZombieEnabled: Boolean = false

    companion object {
        private const val TELEPORT_COOLDOWN = 3

        private val teleportCooldowns = CooldownUtil(TELEPORT_COOLDOWN * 1000L)
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

    object EndermanListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onItemClick(e: PlayerInteractEvent) {
            val player = e.player
            val endermanKit = KitsManager.getKit(player) as? Enderman ?: return
            val rightClicked = e.rightClickItem ?: return
            if (rightClicked.type != Material.COMPASS) return

            endermanKit.teleport(player)
        }
    }
}