package com.jackob.dvz.kits

import org.bukkit.entity.Player

interface Kit {

    fun getName(): String

    fun getPlayer(): Player

    fun onActivate() {
        applyPotionEffects()
        applyAttributeModifiers()
        giveItems()
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
}