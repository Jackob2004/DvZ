package com.jackob.dvz.kits.dwarf.hero

import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.UUID

class Shaman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero), Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createPlayerDisguise("shaman", "Shaman") {
        setItemStack(EquipmentSlot.HEAD, hiddenArmorPiece)
        setItemStack(EquipmentSlot.CHEST, hiddenArmorPiece)
        setItemStack(EquipmentSlot.LEGS, hiddenArmorPiece)
        setItemStack(EquipmentSlot.FEET, hiddenArmorPiece)
    }

    companion object {
        private val hiddenArmorPiece = ItemStack(Material.AIR)
    }

    override fun onActivate() {
        super.onActivate()

        val player = ownerId.toPlayer()!!
        startDisguise(player)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!

        stopDisguise(player)
    }

}