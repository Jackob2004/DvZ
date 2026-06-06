package com.jackob.dvz.kits

import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.entity.Player

interface Disguisable {

    val disguiseTemplate: Disguise

    fun createMobDisguise(type: DisguiseType, name: String = " ", config: LivingWatcher.() -> Unit): Disguise {
        val disguise = MobDisguise(type)
        disguise.setViewSelfDisguise(false)
        disguise.disguiseName = name

        val watcher = disguise.watcher
        watcher.config()

        return disguise
    }

    fun Player.modifyMobDisguise(config: LivingWatcher.() -> Unit) {
        val watcher = DisguiseAPI.getDisguise(this)?.watcher as? LivingWatcher ?: return
        watcher.config()
    }

    fun startDisguise(player: Player) {
        val cloned = disguiseTemplate.clone()
        cloned.entity = player
        cloned.startDisguise()
    }

    fun stopDisguise(player: Player) {
        DisguiseAPI.getDisguise(player)?.stopDisguise()
    }
}