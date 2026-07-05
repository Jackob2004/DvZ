package com.jackob.dvz.core.equipment

import com.jackob.dvz.DvZ
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

object EquipmentRegister {

    private val items: List<CustomItem> = listOf(
        RadianceShroom(),
        RadianceTorch(),
        WigglyWrench(),
        Mortar(),
        SuperMortar(),
    )

    private val itemMap: Map<CustomItemType, CustomItem> = items.associateBy {
        it.type
    }

    fun runOnReceive(type: CustomItemType, player: Player) {
        itemMap[type]?.onReceive(player)
    }

    fun runOnLose(type: CustomItemType, player: Player) {
        itemMap[type]?.onLose(player)
    }

    fun getItem(type: CustomItemType) : ItemStack? {
        return itemMap[type]?.receiveItem()
    }

    fun initRegister() {
        for (item in items) {
            if (item is Listener) {
                DvZ.INSTANCE.server.pluginManager.registerEvents(item, DvZ.INSTANCE)
            }
        }
    }
}