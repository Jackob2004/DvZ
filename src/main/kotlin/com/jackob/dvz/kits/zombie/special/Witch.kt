package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.WitchWatcher
import org.bukkit.event.Listener
import java.util.UUID

class Witch(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero), Disguisable<WitchWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.WITCH) {
        isAggressive = true
    }

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
        WitchListener
    }

    object WitchListener: Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }
    }
}
