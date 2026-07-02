package com.jackob.dvz.kits.zombie.base

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.events.AIZombieSpawnEvent
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.BasePath
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.UpgradeType
import com.jackob.dvz.kits.UpgradesManager
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.name
import com.jackob.dvz.util.toPlayer
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.random.Random

class Zombie(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.ZOMBIE) { }

    override val aiZombieEnabled: Boolean = true

    init {
        ZombieListener
    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        startDisguise(player)

        ZombieListener.upgrades.addPlayer(player)
        ZombieListener.upgrades.applyModifiers(player)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    object ZombieListener : Listener {

        val upgrades = UpgradesManager.create(ZombieUpgrade.entries.size, 1, ZombiePath.entries.size) {
            tier(0) {
                upgrade(ZombieUpgrade.INFECTION_I, UpgradeType.PASSIVE_ABILITY, 1, ZombiePath.UNDEAD) {
                    icon = createItem(Material.POISONOUS_POTATO) {
                        name = "<dark_purple>Infection"
                        description = """
                           <gray>Gives you 2% chance of giving dwarf poison effect I on hitting him.
                           <gray>Effect lasts 5 seconds.
                           <gray>Chance increases each level by 2%.
                        """
                    }

                    (2..6 step 2).forEach { level(it) }

                    action { dwarfVictim, chance ->
                        if (Random.nextInt(100) >= chance) return@action
                        dwarfVictim.addPotionEffect(PotionEffect(PotionEffectType.POISON, 5 * 20, 0))
                    }
                }

                upgrade(ZombieUpgrade.HARDENED_FLESH_I, UpgradeType.MODIFIER, 1, ZombiePath.GIANT) {
                    icon = createItem(Material.COPPER_CHESTPLATE) {
                        name = "<green>Hardened flesh"
                        description = """
                           <gray>Gives you 2 golden hearts.
                           <gray>Increases by 2 each level.
                        """
                    }

                    (0..2).forEach { level(it) }

                    action { zombiePlayer, modifier ->
                        zombiePlayer.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, Int.MAX_VALUE, modifier))
                    }
                }

                upgrade(ZombieUpgrade.LEADERSHIP_I, UpgradeType.PASSIVE_ABILITY, 1, ZombiePath.CAPTAIN) {
                    icon = createItem(Material.GOLDEN_HELMET) {
                        name = "<gold>Leadership"
                        description = """
                           <gray>Half of your AI zombies spawned around you will
                           <gray>receive buff of 2 golden hearts.
                           <gray>Increases by 2 each level.
                        """
                    }

                    (0..2).forEach { level(it) }

                    action { zombieEntity, modifier ->
                        zombieEntity.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, Int.MAX_VALUE, modifier))
                    }
                }

                upgrade(ZombieUpgrade.RAW_DAMAGE_I, UpgradeType.MODIFIER, 1) {
                    icon = createItem(Material.IRON_SWORD) {
                        name = "<gold>Raw damage"
                        description = """
                           <gray>Increases base attack damage by 1 heart each level
                        """
                    }

                    (2..6 step 2).forEach { level(it) }

                    action { zombiePlayer , modifier ->
                        val attr = zombiePlayer.getAttribute(Attribute.ATTACK_DAMAGE) ?: return@action
                        attr.baseValue += modifier
                    }
                }

            }

        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onZombieDealDamage(e: EntityDamageByEntityEvent) {
            val attacker = e.damager as? Player ?: return
            val upgrade = ZombieUpgrade.INFECTION_I
            if (!upgrades.hasUpgrade(attacker, upgrade)) return
            if (KitsManager.getKit(attacker) !is Zombie) return

            val dwarfVictim = e.entity as? Player ?: return
            upgrades.applyAbility(dwarfVictim, upgrade, 0)
        }

        @EventHandler
        fun onAIZombieSpawn(e: AIZombieSpawnEvent) {
            val player = e.zombiePlayer
            val upgrade = ZombieUpgrade.LEADERSHIP_I
            if (!upgrades.hasUpgrade(player, upgrade)) return
            if (KitsManager.getKit(player) !is Zombie) return

            for (i in 0..(e.zombies.size / 2).minus(1)) {
                upgrades.applyAbility(e.zombies[i], upgrade, 0)
            }
        }

        enum class ZombieUpgrade {
            INFECTION_I,
            INFECTION_II,
            INFECTION_III,
            HARDENED_FLESH_I,
            HARDENED_FLESH_II,
            HARDENED_FLESH_III,
            LEADERSHIP_I,
            LEADERSHIP_II,
            LEADERSHIP_III,
            RAW_DAMAGE_I,
            RAW_DAMAGE_II,
            RAW_DAMAGE_III,
            RAW_DAMAGE_IV,
        }

        enum class ZombiePath(override val pathName: String) : BasePath {
            UNDEAD("<i><dark_red>Undead"),
            GIANT("<i><dark_green>Giant"),
            CAPTAIN("<i><gold>Captain"),
        }
    }
}