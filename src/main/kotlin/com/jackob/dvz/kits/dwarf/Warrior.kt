package com.jackob.dvz.kits.dwarf

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class Warrior(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero) {

    private var fallDamageOnTask: BukkitTask? = null

    init {
        WarriorListener
    }

    override fun onDeactivate() {
        super.onDeactivate()
        leapCooldowns.removeFromCooldown(ownerId.toPlayer()!!)
        if (fallDamageOnTask != null) {
            fallDamageOnTask!!.cancel()
            fallDamageOnTask = null
        }
    }

    companion object {
        private const val NO_FALL_DAMAGE_TIME = 4

        private val leapCooldowns = CooldownUtil(15 * 1000)
    }

    private fun turnFallDamageOn(p: Player) {
        val attr = p.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)
        attr?.baseValue = attr.defaultValue
    }

    private fun leapAbility(player: Player) {
        if (leapCooldowns.isOnCooldown(player)) {
            leapCooldowns.displayCooldown(player)
        } else {
            player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER)?.baseValue = 0.0

            val directionVector = player.location.direction.normalize().multiply(2.8)
            directionVector.y = 0.9
            player.velocity = directionVector

            player.playSound(player.location, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1f)

            fallDamageOnTask = sync(delay = TimeUnit.SECONDS(NO_FALL_DAMAGE_TIME.toLong())) {
                ownerId.toPlayer()?.takeIf { it.isOnline }?.let { p ->
                    turnFallDamageOn(p)
                }
                fallDamageOnTask = null
            }
        }
    }

    private fun handleOnQuit(player: Player) {
        if (fallDamageOnTask != null) {
            fallDamageOnTask!!.cancel()
            fallDamageOnTask = null
        }
        turnFallDamageOn(player)
    }

    object WarriorListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onSwordClick(e: PlayerInteractEvent) {
            val item = e.rightClickItem ?: return
            if (item.type != Material.DIAMOND_AXE) return
            val player = e.player
            val kit = KitsManager.getKit(player) as? Warrior ?: return

            kit.leapAbility(player)
        }

        @EventHandler
        fun onPlayerQuit(e: PlayerQuitEvent) {
            val player = e.player
            val kit = KitsManager.getKit(player) as? Warrior ?: return

            kit.handleOnQuit(player)
        }
    }

}