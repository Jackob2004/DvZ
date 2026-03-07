package com.jackob.dvz.util

import org.bukkit.Registry
import org.bukkit.entity.Player

fun Player.resetAllAttributes() {
    Registry.ATTRIBUTE.forEach { attribute ->
        getAttribute(attribute)?.let { instance ->
            instance.baseValue = instance.baseValue

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
}