package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.equipment.ExplosiveCharge
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.UUID

private const val EXPLOSION_RANGE = 6.0

class SuperCreeper(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<CreeperWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CREEPER) {
        isPowered = true
    }

    override val aiZombieEnabled: Boolean = true

    private val explosive: ExplosiveCharge =
        ExplosiveCharge(::startIgnition, ::stopIgnition, ::explode, 4.5f, owner)

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!

        startDisguise(player)
        player.inventory.addItem(explosive.receiveItem())
        explosive.onReceive(player)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!

        stopDisguise(player)
        explosive.onLose(player)
    }

    private fun explode(player: Player) {
        val location = player.location
        val updatedVelocity = Vector(0.0, 4.0, 0.0)
        for (p in location.getNearbyPlayers(EXPLOSION_RANGE)) {
            if (GameManager.getPlayerTeam(p) != TeamType.DWARF) continue

            p.velocity = updatedVelocity
        }
    }

    private fun startIgnition(player: Player) {
        player.modifyMobDisguise {
            isIgnited = true
        }
    }

    private fun stopIgnition(player: Player) {
        player.modifyMobDisguise {
            isIgnited = false
        }
    }

}