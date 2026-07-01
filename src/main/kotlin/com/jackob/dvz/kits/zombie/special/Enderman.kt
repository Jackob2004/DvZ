package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.EndermanWatcher
import org.bukkit.event.Listener
import java.util.UUID

class Enderman(internalName: String, owner: UUID) : BaseKit(internalName, owner), Disguisable<EndermanWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.ENDERMAN) { }

    override val isHero: Boolean = true

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
        EndermanListener
    }

    object EndermanListener: Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }
}