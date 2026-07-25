package com.jackob.dvz.kits

import com.jackob.dvz.core.equipment.CustomItemType
import com.jackob.dvz.core.equipment.EquipmentRegister
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.toPlayer
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Classes extending BaseKit are expected to provide ctor perfectly matching superclass ctor arguments(order and types).
 */
abstract class BaseKit(val internalName: String, val ownerId: UUID, val isHero: Boolean) {

    /**
     * Indicates whether AI zombies can be spawned around the owner of the kit.
     * Null means the attribute is not applicable to the given kit.
     */
    open val aiZombieEnabled: Boolean? = null

    val kitAttributes: KitAttributes = KitConfigsCache.retrieveKitAttributes(internalName)

    open fun onActivate() {
        applyPotionEffects()
        applyAttributeModifiers()
        giveItems()
        sendActivationMessage()

        val player = ownerId.toPlayer()!!
        if (aiZombieEnabled == null) {
            val ale = CustomItemType.HEALING_ALE

            player.inventory.addItem(EquipmentRegister.getItem(ale)!!)
            EquipmentRegister.runOnReceive(ale, ownerId.toPlayer()!!)
        } else if (aiZombieEnabled != null) {
            player.inventory.setItem(17, EquipmentRegister.getItem(CustomItemType.DEATH_SCROLL)!!)
        }
    }

    open fun onDeactivate() {
        if (aiZombieEnabled == null) {
            val ale = CustomItemType.HEALING_ALE
            EquipmentRegister.runOnLose(ale, ownerId.toPlayer()!!)
        }
    }

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