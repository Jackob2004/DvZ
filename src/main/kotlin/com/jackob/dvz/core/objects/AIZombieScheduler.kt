package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.TeamType
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityTargetEvent
import org.bukkit.inventory.ItemStack

class AIZombieScheduler : Listener {

    companion object {
        private val MOB_TYPE = EntityType.VINDICATOR
    }

    private fun spawnZombie(loc: Location): Entity {
        val zombieDisguise = MobDisguise(DisguiseType.ZOMBIE)
        val watcher = zombieDisguise.watcher
        watcher.itemInMainHand = ItemStack(Material.WOODEN_SWORD)

        DisguiseAPI.disguiseNextEntity(zombieDisguise)

        val zombie = (loc.world.spawnEntity(loc, MOB_TYPE) as LivingEntity).apply {
            val healthVal = 40.0

            getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.40
            getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue = 0.35
            getAttribute(Attribute.MAX_HEALTH)?.baseValue = healthVal

            health = healthVal
        }

        return zombie
    }

    fun startScheduling() {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun stopScheduling() {
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