package com.jackob.dvz.kits

import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.entity.Player

interface Disguisable<T: LivingWatcher> {

    val disguiseTemplate: Disguise

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
    fun createPlayerDisguise(skinName: String, nickname: String = " ", config: T.() -> Unit): Disguise {
        val disguise = PlayerDisguise(nickname)
        disguise.setViewSelfDisguise(false)

        disguise.skin = skinName

        val watcher = disguise.watcher as? T
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