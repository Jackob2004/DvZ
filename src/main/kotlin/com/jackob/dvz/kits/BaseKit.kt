package com.jackob.dvz.kits

import com.jackob.dvz.util.mm
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Classes extending BaseKit are expected to provide ctor perfectly matching superclass ctor arguments(order and types).
 */
abstract class BaseKit(val internalName: String, val owner: Player) {

    open fun onActivate() {
        applyPotionEffects()
        applyAttributeModifiers()
        giveItems()
        sendActivationMessage()
    }

    open fun onDeactivate() { }

    protected fun applyPotionEffects() {
        owner.addPotionEffects(KitConfigsCache.retrieveKitPotions(internalName))
    }

    protected fun applyAttributeModifiers() {
        KitConfigsCache.retrieveAttributes(internalName).forEach {
            owner.getAttribute(it.first)!!.addModifier(it.second)
        }
    }

    protected fun giveItems() {
        KitConfigsCache.retrieveKitItems(internalName).forEach {
            owner.inventory.addItem(it)
        }

        KitConfigsCache.retrieveKitCustomItems(internalName).forEach {
            owner.inventory.addItem(it)
        }
    }

    protected fun sendActivationMessage() {
        val message = KitConfigsCache.retrieveActivationMessages(internalName)

        if (message.second) {
            Bukkit.broadcast(message.first.mm())
        } else {
            owner.sendMessage(message.first.mm())
        }
    }
}