package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Spider(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CAVE_SPIDER) { }

    override val aiZombieEnabled: Boolean = false

    companion object {
        private val biteItem = Material.SPIDER_EYE

        private val bitePoisonEffect = PotionEffect(PotionEffectType.POISON, 6 * 20, 0, true, true)

        private val biteNauseaEffect = PotionEffect(PotionEffectType.NAUSEA, 6 * 20, 0, true, true)
    }

    init {
        SpiderListener
    }

    override fun onActivate() {
        super.onActivate()
        startDisguise(ownerId.toPlayer()!!)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    private fun bitePassiveAbility(dwarfVictim: Player) {
        dwarfVictim.addPotionEffect(biteNauseaEffect)
        dwarfVictim.addPotionEffect(bitePoisonEffect)
    }

    object SpiderListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onDwarfHit(e: EntityDamageByEntityEvent) {
            val spiderPlayer = e.damager as? Player ?: return
            val spiderKit = KitsManager.getKit(spiderPlayer) as? Spider ?: return
            if (spiderPlayer.inventory.itemInMainHand.type != biteItem) return

            val dwarfVictim = e.entity as? Player ?: return
            if (GameManager.getPlayerTeam(dwarfVictim) != TeamType.DWARF) return

            spiderKit.bitePassiveAbility(dwarfVictim)
        }

    }

}
