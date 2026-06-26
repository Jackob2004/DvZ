package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.isInRegion
import com.jackob.dvz.util.sync
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Vindicator
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import java.util.LinkedList
import java.util.Queue

class AIZombieScheduler(private val onlinePlayers: Collection<Player>, regions: RegionManager) : Listener {

    private val zombieQueue: Queue<Entity> = LinkedList()

    private val spawnCooldowns = CooldownUtil(18_000)

    private var task: BukkitTask? = null

    private val zombieArea: ProtectedRegion = regions.getRegion("zombie-area")!!

    companion object {
        private val MOB_TYPE = EntityType.VINDICATOR

        private val SPAWN_CORD_MODIFIERS: Array<DoubleArray> = arrayOf(
            doubleArrayOf(-3.0, 3.0),
            doubleArrayOf(-3.0, -3.0),
            doubleArrayOf(3.0, 3.0),
            doubleArrayOf(3.0, -3.0),
        )
    }

    private fun spawnZombie(loc: Location): Entity {
        val zombieDisguise = MobDisguise(DisguiseType.ZOMBIE)
        val watcher = zombieDisguise.watcher
        watcher.itemInMainHand = ItemStack(Material.WOODEN_SWORD)

        DisguiseAPI.disguiseNextEntity(zombieDisguise)

        val zombie = loc.world.spawn(loc, Vindicator::class.java) {
            it.apply {
                val healthVal = 40.0

                getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.40
                getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 0.35
                getAttribute(Attribute.MAX_HEALTH)?.baseValue = healthVal
                getAttribute(Attribute.FOLLOW_RANGE)?.baseValue = 25.0

                health = healthVal
                isPatrolLeader = false
                equipment.helmet = null
                canPickupItems = false
            }
        }

        return zombie
    }

    private fun spawnZombies(player: Player) {
        val loc = player.location

        for (i in 0..3) {
            val updatedLoc = loc.clone().add(SPAWN_CORD_MODIFIERS[i][0], 0.0, SPAWN_CORD_MODIFIERS[i][1])
            zombieQueue.add(spawnZombie(updatedLoc))
        }
    }

    private fun removeOldZombies() {
        val oldestZombie = zombieQueue.peek() ?: return
        if (oldestZombie.isValid && oldestZombie.ticksLived < TimeUnit.SECONDS(10)) return

        val zombiesToRemove = (30.0 * zombieQueue.size / 100).toInt()

        if (zombiesToRemove < 1) return

        for (i in 1..zombiesToRemove) {
            if (zombieQueue.isEmpty()) break

            val zombie = zombieQueue.remove()
            if (zombie.isValid) {
                zombie.remove()
            }
        }
    }

    fun startScheduling() {
        check(task == null) { "Scheduler cannot be started twice!" }

        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)

        val spawningRange = 10.0
        val cleanupInterval = 12

        var cleanupTimer = 0

        task = sync(period = TimeUnit.SECONDS(1)) {
            for (player in onlinePlayers) {
                val kit = KitsManager.getKit(player) ?: continue
                if (kit.aiZombieEnabled != true) continue
                if (spawnCooldowns.isOnCooldownSafe(player)) continue
                if (player.isInRegion(zombieArea)) continue

                val anyDwarfNearby = player.getNearbyEntities(spawningRange, spawningRange, spawningRange)
                    .any { it is Player && GameManager.getPlayerTeam(it) == TeamType.DWARF }

                if (anyDwarfNearby) {
                    spawnZombies(player)
                    spawnCooldowns.putOnCooldown(player)
                }
            }
            cleanupTimer++

            if (cleanupTimer >= cleanupInterval) {
                cleanupTimer = 0
                removeOldZombies()
            }
        }
    }

    fun stopScheduling() {
        task?.cancel()
        task = null
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun onZombieTarget(event: EntityTargetEvent) {
        if (event.entity.type != MOB_TYPE) return

        val target = event.target as? Player ?: return
        if (GameManager.getPlayerTeam(target) == TeamType.DWARF) return

        event.isCancelled = true
    }

    @EventHandler
    fun onZombieDamage(e: EntityDamageByEntityEvent) {
        if (e.entity.type != MOB_TYPE) return

        val attacker = e.damageSource.causingEntity as? Player ?: return

        if (GameManager.getPlayerTeam(attacker) != TeamType.DWARF) {
            e.isCancelled = true
        }
    }

}