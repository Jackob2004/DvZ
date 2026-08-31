package com.jackob.dvz.kits

import com.jackob.dvz.kits.dwarf.hero.Shaman
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

interface Disguisable<T: LivingWatcher> {

    val disguiseTemplate: Disguise

    companion object {
        private val hiddenArmorPiece = ItemStack(Material.AIR)
    }

    @Suppress("UNCHECKED_CAST")
    fun createMobDisguise(type: DisguiseType, name: String = " ", config: T.() -> Unit): Disguise {
        val disguise = MobDisguise(type)
        disguise.setViewSelfDisguise(false)
        disguise.disguiseName = name

        val watcher = disguise.watcher as? T
        watcher?.config()

        return disguise
    }

    @Suppress("UNCHECKED_CAST")
    fun createPlayerDisguise(skinName: String, nickname: String = " ", hideArmor: Boolean = true, config: T.() -> Unit): Disguise {
        val disguise = PlayerDisguise(nickname)
        disguise.setViewSelfDisguise(false)

        disguise.skin = skinName

        val watcher = disguise.watcher as? T
        if (watcher != null && hideArmor) {
            watcher.setItemStack(EquipmentSlot.HEAD, hiddenArmorPiece)
            watcher.setItemStack(EquipmentSlot.CHEST,hiddenArmorPiece)
            watcher.setItemStack(EquipmentSlot.LEGS, hiddenArmorPiece)
            watcher.setItemStack(EquipmentSlot.FEET, hiddenArmorPiece)
        }
        watcher?.config()

        return disguise
    }

    @Suppress("UNCHECKED_CAST")
    fun Player.modifyMobDisguise(config: T.() -> Unit) {
        val watcher = DisguiseAPI.getDisguise(this)?.watcher as? T ?: return
        watcher.config()
    }

    fun startDisguise(player: Player) {
        startDisguise(player, disguiseTemplate)
    }

    fun startDisguise(player: Player, template: Disguise) {
        val cloned = template.clone()
        cloned.entity = player
        cloned.startDisguise()
    }

    fun stopDisguise(player: Player) {
        DisguiseAPI.getDisguise(player)?.stopDisguise()
    }
}