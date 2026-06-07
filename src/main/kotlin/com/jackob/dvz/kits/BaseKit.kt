package com.jackob.dvz.kits

import com.jackob.dvz.util.mm
import com.jackob.dvz.util.toPlayer
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Classes extending BaseKit are expected to provide ctor perfectly matching superclass ctor arguments(order and types).
 */
abstract class BaseKit(val internalName: String, val ownerId: UUID) {

    abstract val isHero: Boolean

    open fun onActivate() {
        applyPotionEffects()
        applyAttributeModifiers()
        giveItems()
        sendActivationMessage()
    }

    open fun onDeactivate() { }

    protected fun applyPotionEffects() {
        ownerId.toPlayer()?.addPotionEffects(KitConfigsCache.retrieveKitPotions(internalName))
    }

    protected fun applyAttributeModifiers() {
        KitConfigsCache.retrieveAttributes(internalName).forEach {
            ownerId.toPlayer()?.getAttribute(it.first)!!.addModifier(it.second)
        }
    }

    protected fun giveItems() {
        KitConfigsCache.retrieveKitItems(internalName).forEach {
            ownerId.toPlayer()?.inventory?.addItem(it)
        }

        KitConfigsCache.retrieveKitCustomItems(internalName).forEach {
            ownerId.toPlayer()?.inventory?.addItem(it)
        }
    }

    protected fun sendActivationMessage() {
        val message = KitConfigsCache.retrieveActivationMessages(internalName)

        if (message.second) {
            Bukkit.broadcast(message.first.mm())
        } else {
            ownerId.toPlayer()?.sendMessage(message.first.mm())
        }
    }
}