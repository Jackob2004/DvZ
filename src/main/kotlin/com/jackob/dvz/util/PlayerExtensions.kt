package com.jackob.dvz.util

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

private fun Player.resetAllAttributes() {
    Registry.ATTRIBUTE.forEach { attribute ->
        getAttribute(attribute)?.let { instance ->
            instance.baseValue = instance.defaultValue

            instance.modifiers.forEach { modifier ->
                instance.removeModifier(modifier)
            }
        }
    }
}

/**
 * Hard resets the player to a default vanilla state.
 * * Clears inventory, potion effects, and custom attributes
 * and restores base health, food, and movement/flying speeds.
 */
fun Player.resetAll() {
    inventory.clear()
    activePotionEffects.forEach { potionEffect ->
        removePotionEffect(potionEffect.type)
    }
    resetAllAttributes()
    health = 20.0
    walkSpeed = 0.2F
    flySpeed = 0.1F
    foodLevel = 20
    isFlying = false
    isInvisible = false
    allowFlight = false
    isCollidable = true
    gameMode = GameMode.SURVIVAL
    level = 0
    exp = 0f
}

fun UUID.toPlayer() : Player? = Bukkit.getPlayer(this)

fun Player.removeItem(item: ItemStack, amountToRemove: Int) {
    if (item.amount - amountToRemove <= 0) {
        inventory.remove(item)
    } else {
        item.amount -= amountToRemove
    }
}

fun Entity.isInRegion(region: ProtectedRegion): Boolean {
    val loc = location

    val weVector = BlockVector3.at(loc.x, loc.y, loc.z)

    return region.contains(weVector)
}

fun Player.launchPlayer(force: Double, yValue: Double) {
    val directionVector = location.direction.normalize().multiply(force)
    directionVector.y = yValue
    velocity = directionVector
}

inline fun Player.withCooldown(cooldown: CooldownUtil, action: Player.() -> Unit) {
    if (cooldown.isOnCooldown(this)) {
        cooldown.displayCooldown(this)
    } else {
        this.action()
    }
}