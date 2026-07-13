package com.jackob.dvz.kits.zombie.base

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.event.Listener
import java.util.UUID

class Creeper(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<CreeperWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CREEPER) { }

    override val aiZombieEnabled: Boolean = true

    override fun onActivate() {
        super.onActivate()
        startDisguise(ownerId.toPlayer()!!)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    init {
        CreeperListener
    }

    object CreeperListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

    }
}
