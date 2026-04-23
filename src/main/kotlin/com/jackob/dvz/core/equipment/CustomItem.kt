package com.jackob.dvz.core.equipment

import com.jackob.dvz.DvZ
import com.jackob.dvz.util.updateItem
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class CustomItem {

    abstract val item: ItemStack

    abstract val type: CustomItemType

    protected fun isCustomItem(item: ItemStack): Boolean {
        return item.persistentDataContainer.get(ITEM_KEY, PersistentDataType.STRING) == type.toString()
    }

    fun retrieveItem(): ItemStack {
        item.updateItem { persistentDataContainer.set(ITEM_KEY, PersistentDataType.STRING, type.toString()) }
        return item
    }

    companion object {
        val ITEM_KEY = NamespacedKey(DvZ.INSTANCE, "dvz_item")
    }

}