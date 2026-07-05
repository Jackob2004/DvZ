package com.jackob.dvz.core.equipment

import com.jackob.dvz.DvZ
import com.jackob.dvz.util.updateItem
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class CustomItem {

    abstract val item: ItemStack

    abstract val type: CustomItemType

    protected fun isCustomItem(item: ItemStack): Boolean {
        return item.persistentDataContainer.get(ITEM_KEY, PersistentDataType.STRING) == type.toString()
    }

    fun receiveItem(): ItemStack {
        item.updateItem { persistentDataContainer.set(ITEM_KEY, PersistentDataType.STRING, type.toString()) }
        return item
    }

    open fun onReceive(player: Player) {}

    open fun onLose(player: Player) {}

    companion object {
        val ITEM_KEY = NamespacedKey(DvZ.INSTANCE, "dvz_item")
    }

}