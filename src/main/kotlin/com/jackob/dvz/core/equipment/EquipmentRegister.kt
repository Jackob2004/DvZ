package com.jackob.dvz.core.equipment

import com.jackob.dvz.DvZ
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

object EquipmentRegister {

    private val items: List<CustomItem> = listOf(
        RadianceShroom(),
        RadianceTorch()
    )

    private val itemMap: Map<CustomItemType, CustomItem> = items.associateBy {
        it.type
    }

    fun getItem(type: CustomItemType) : ItemStack? {
        return itemMap[type]?.retrieveItem()
    }

    fun initRegister() {
        for (item in items) {
            if (item is Listener) {
                DvZ.INSTANCE.server.pluginManager.registerEvents(item, DvZ.INSTANCE)
            }
        }
    }
}