package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.ShrineDamageEvent
import com.jackob.dvz.core.events.ShrineFallEvent
import com.jackob.dvz.core.events.ShrineGoldDepositEvent
import com.jackob.dvz.core.events.ShrineTrespassEvent
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.isInRegion
import com.jackob.dvz.util.removeItem
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class Shrine(
    private val maxShield: Int,
    private val maxHealth: Int,
    private val regenRate: Int,
    private val damageRate: Int,
    private val shrineNumber: Int,
    regionManager: RegionManager
) : Listener {

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

    private val zombieBuffer = ArrayList<Player>()

    init {
        if (currentState == ShrineState.ACTIVE) {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }

    private fun takeDamage(zombies: Collection<Player>) {
        if (currentShield > 0) {
            currentShield -= damageRate
        } else {
            currentHealth -= damageRate
            Bukkit.getPluginManager().callEvent(ShrineDamageEvent(zombies.toList()))
        }

        if (currentHealth == 0) {
            currentState = ShrineState.FALLEN
            HandlerList.unregisterAll(this)
            Bukkit.getPluginManager().callEvent(ShrineFallEvent(shrineNumber))
        }
    }

    private fun applyRegen() {
        if (currentShield > 0) return

        currentHealth += regenRate
    }

    private fun handleActiveState(players: Collection<Player>) {
        zombieBuffer.clear()
        var dwarfCount = 0

        for (player in players) {
            if (!KitsManager.hasKit(player) || !player.isInRegion(innerShrine)) continue

            val team = GameManager.getPlayerTeam(player)
            if (team == TeamType.ZOMBIE) {
                zombieBuffer.add(player)
            } else if (team == TeamType.DWARF) {
                dwarfCount++
            }
        }

        val zombieCount = zombieBuffer.size

        if (zombieCount == 0 && dwarfCount > 0) {
            applyRegen()
        } else if (zombieCount > 0) {
            takeDamage(zombieBuffer)
        }
    }

    private fun handleInactiveState(players: Collection<Player>, aiZombies: Collection<LivingEntity>) {
        for (p in players) {
            if (KitsManager.hasKit(p) && GameManager.getPlayerTeam(p) == TeamType.ZOMBIE && p.isInRegion(outerShrine)) {
                Bukkit.getPluginManager().callEvent(ShrineTrespassEvent(p))
            }
        }

        for (zombie in aiZombies) {
            if (zombie.isValid && !zombie.isDead && zombie.isInRegion(outerShrine)) {
                Bukkit.getPluginManager().callEvent(ShrineTrespassEvent(zombie))
            }
        }
    }

    /**
     * @param players collection of players expected to be on the same word the shrine is
     */
    fun onUpdate(players: Collection<Player>, aiZombies: Collection<LivingEntity>) {
        when (currentState) {
            ShrineState.ACTIVE -> handleActiveState(players)
            ShrineState.INACTIVE -> handleInactiveState(players, aiZombies)
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
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    @EventHandler
    fun onShrineClick(e: PlayerInteractEvent) {
        if (e.action != Action.RIGHT_CLICK_BLOCK) return
        if (e.clickedBlock?.type != Material.END_PORTAL_FRAME) return
        if (e.hand != EquipmentSlot.HAND) return

        val player = e.player
        if (GameManager.getPlayerTeam(player) != TeamType.DWARF) return

        val item = player.inventory.itemInMainHand
        if (item.type != Material.GOLD_INGOT) return
        if (!player.isInRegion(innerShrine)) return

        val amount = 1
        player.removeItem(item, amount)
        Bukkit.getPluginManager().callEvent(ShrineGoldDepositEvent(player, amount))
    }

    private enum class ShrineState {
        ACTIVE,
        INACTIVE,
        FALLEN
    }

    data class ShrineData(val health: Int, val maxHealth: Int, val shielded: Boolean)
}