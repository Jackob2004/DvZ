package com.jackob.dvz.kits

import com.jackob.dvz.util.mm
import org.bukkit.Bukkit
import org.bukkit.entity.Player

interface Kit {

    fun getName(): String

    fun getPlayer(): Player

    fun onActivate() {
        applyPotionEffects()
        applyAttributeModifiers()
        giveItems()
        sendActivationMessage()
    }

    fun onDeactivate() { }

    fun applyPotionEffects() {
        getPlayer().addPotionEffects(KitConfigsCache.retrieveKitPotions(getName()))
    }

    fun applyAttributeModifiers() {
        KitConfigsCache.retrieveAttributes(getName()).forEach {
            getPlayer().getAttribute(it.first)!!.addModifier(it.second)
        }
    }

    fun giveItems() {
        KitConfigsCache.retrieveKitItems(getName()).forEach {
            getPlayer().inventory.addItem(it)
        }
    }

    fun sendActivationMessage() {
        val message = KitConfigsCache.retrieveActivationMessages(getName())

        if (message.second) {
            Bukkit.broadcast(message.first.mm())
        } else {
            getPlayer().sendMessage(message.first.mm())
        }
    }
}