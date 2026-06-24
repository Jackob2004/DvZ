package com.jackob.dvz.core.objects

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.ShrineDamageEvent
import com.jackob.dvz.core.events.ShrineFallEvent
import com.jackob.dvz.core.events.ShrineTrespassEvent
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Shrine(
    private val maxShield: Int,
    private val maxHealth: Int,
    private val regenRate: Int,
    private val damageRate: Int,
    private val shrineNumber: Int,
    regionManager: RegionManager
) {

    private var currentState: ShrineState = if (shrineNumber == 0) ShrineState.ACTIVE else ShrineState.INACTIVE

    private var currentShield: Int = maxShield
        set(value) {
            field = value.coerceIn(0, maxShield)
        }

    private var currentHealth: Int = maxHealth
        set(value) {
            field = value.coerceIn(0, maxHealth)
        }

    private val innerShrine: ProtectedRegion = regionManager.getRegion("inner-shrine-${shrineNumber.plus(1)}")!!

    private val outerShrine: ProtectedRegion = regionManager.getRegion("outer-shrine-${shrineNumber.plus(1)}")!!

    private fun isInRegion(region: ProtectedRegion, player: Player): Boolean {
        val loc = player.location

        val weVector = BlockVector3.at(loc.x, loc.y, loc.z)

        return region.contains(weVector)
    }

    private fun takeDamage(zombies: Collection<Player>) {
        if (currentShield > 0) {
            currentShield -= damageRate
        } else {
            currentHealth -= damageRate
            Bukkit.getPluginManager().callEvent(ShrineDamageEvent(zombies))
        }

        if (currentHealth == 0) {
            currentState = ShrineState.FALLEN
            Bukkit.getPluginManager().callEvent(ShrineFallEvent(shrineNumber))
        }
    }

    private fun applyRegen() {
        if (currentShield > 0) return

        currentHealth += regenRate
    }

    private fun handleActiveState(players: Collection<Player>) {
        val (zombies, dwarves) = players
            .filter { isInRegion(innerShrine, it) }
            .partition { GameManager.getPlayerTeam(it) == TeamType.ZOMBIE }

        val zombieCount = zombies.size
        val dwarfCount = dwarves.size

        if (zombieCount == 0 && dwarfCount > 0) {
            applyRegen()
        } else if (zombieCount > 0) {
            takeDamage(zombies)
        }
    }

    private fun handleInactiveState(players: Collection<Player>) {
        players.filter {
            GameManager.getPlayerTeam(it) == TeamType.ZOMBIE && isInRegion(outerShrine, it)
        }.forEach {
            Bukkit.getPluginManager().callEvent(ShrineTrespassEvent(it))
        }
    }

    /**
     * @param players collection of players expected to be on the same word the shrine is
     */
    fun onUpdate(players: Collection<Player>) {
        when (currentState) {
            ShrineState.ACTIVE -> handleActiveState(players)
            ShrineState.INACTIVE -> handleInactiveState(players)
            ShrineState.FALLEN -> Unit
        }
    }

    fun getShrineData(): ShrineData {
        val shielded = currentShield > 0
        val health = if (shielded) currentShield else currentHealth
        val maxHealth = if (shielded) maxShield else maxHealth

        return ShrineData(health, maxHealth, shielded)
    }

    fun activateShrine() {
        check(currentState == ShrineState.INACTIVE) { "You can only activate inactive shrine!!!" }
        currentState = ShrineState.ACTIVE
    }

    private enum class ShrineState {
        ACTIVE,
        INACTIVE,
        FALLEN
    }

    data class ShrineData(val health: Int, val maxHealth: Int, val shielded: Boolean)
}