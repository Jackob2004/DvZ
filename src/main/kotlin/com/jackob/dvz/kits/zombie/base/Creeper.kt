package com.jackob.dvz.kits.zombie.base

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class Creeper(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<CreeperWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CREEPER) { }

    override val aiZombieEnabled: Boolean = true

    private var explosionTask: BukkitTask? = null

    init {
        CreeperListener
    }

    override fun onActivate() {
        super.onActivate()
        startDisguise(ownerId.toPlayer()!!)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    private fun startIgnition(player: Player) {
        if (explosionTask != null) return

        val timeToExplosion = TimeUnit.SECONDS(3)
        val world = player.world

        var timer = timeToExplosion
        explosionTask = sync(period = TimeUnit.TICKS(1)) {
            player.exp = (timer * 100 / timeToExplosion / 100f).coerceIn(0f, 1f)

            timer--
            if (timer <= 0) {
                cancel()
                explosionTask = null

                val location = player.location.add(0.0, 1.0, 0.0)
                world.createExplosion(location, 3f, false, true, player)
                player.health = 0.0
            }
        }

        player.modifyMobDisguise {
            isPowered = true
        }
        world.playSound(player.location, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)
    }

    private fun stopIgnition(player: Player) {
        if (explosionTask != null) {
            explosionTask!!.cancel()
            explosionTask = null

            player.exp = 0f
            player.modifyMobDisguise {
                isPowered = false
            }
            player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f)
        }
    }


    object CreeperListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onGunpowderClick(e: PlayerInteractEvent) {
            val player = e.player
            val creeperKit = KitsManager.getKit(player) as? Creeper ?: return

            if (e.rightClickItem?.type == Material.GUNPOWDER) {
                creeperKit.startIgnition(player)
            } else if (e.leftClickItem?.type == Material.GUNPOWDER) {
                creeperKit.stopIgnition(player)
            }

        }
    }
}
