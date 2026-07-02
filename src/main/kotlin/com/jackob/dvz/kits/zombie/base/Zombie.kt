package com.jackob.dvz.kits.zombie.base

import com.jackob.dvz.DvZ
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
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID

class Zombie(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero), Disguisable<LivingWatcher> {

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

        val upgrades = UpgradesManager.create(ZombieUpgrade.entries.size, 2, ZombiePath.entries.size) {
            tier(0) {

                upgrade(ZombieUpgrade.UNDEAD_I, UpgradeType.MODIFIER, 6, ZombiePath.UNDEAD) {
                    icon = createItem(Material.REDSTONE) {
                        name = "<blue>Undead"
                        description = """
                           <gray>Gives you a chance to be reborn in the placed you last died
                        """
                    }

                    level(1)
                    level(2)

                    action { player, modifier ->
                        player.inventory.addItem(createItem(Material.RED_STAINED_GLASS) {
                            name = "<red>Undead $modifier"
                        })
                    }
                }

                upgrade(ZombieUpgrade.GIANT_I,UpgradeType.MODIFIER, 7, ZombiePath.GIANT) {
                    icon = createItem(Material.YELLOW_STAINED_GLASS) {
                        name = "<blue>Giant"
                        description = """
                           <gray>Makes you bigger, slower and stronger zombie
                        """
                    }

                    level(1)
                    level(2)

                    action { player, modifier ->
                        player.inventory.addItem(createItem(Material.YELLOW_STAINED_GLASS) {
                            name = "<yellow>Giant $modifier"
                        })
                    }
                }

                upgrade(ZombieUpgrade.CAPTAIN_I, UpgradeType.MODIFIER, 4, ZombiePath.CAPTAIN) {
                    icon = createItem(Material.BLACK_BANNER) {
                        name = "<blue>Captain"
                        description = """
                           <gray>Gives you ability to buff other zombies
                        """
                    }

                    level(1)

                    action { player, modifier ->
                        player.inventory.addItem(createItem(Material.BLACK_BANNER) {
                            name = "<red>Captain$modifier"
                        })
                    }
                }
                upgrade(ZombieUpgrade.TOUGH_FLESH_I, UpgradeType.MODIFIER, 2) {
                    icon = createItem(Material.ROTTEN_FLESH) {
                        name = "<dark_green>Tough Flesh"
                        description = """
                           <gray>Increases number of hearts by one on each level 
                        """
                    }

                    (2..6 step 2).forEach { level(it) }

                    action { player, modifier ->
                        val maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH) ?: return@action
                        maxHealthAttr.baseValue += modifier
                        player.health = maxHealthAttr.value
                    }
                }

            }

            tier(1) {
                upgrade(ZombieUpgrade.RAW_DAMAGE_I, UpgradeType.MODIFIER, 3, ZombiePath.UNDEAD) {
                    icon = createItem(Material.COPPER_SWORD) {
                        name = "<dark_red>Raw Damage"
                        description = """
                           <gray>Increases zombie's base damage
                        """
                    }

                    level(1.0)
                    level(3.0)

                    action { player, modifier ->
                        val maxAttackAttr = player.getAttribute(Attribute.ATTACK_DAMAGE) ?: return@action
                        maxAttackAttr.baseValue += modifier
                    }
                }

                upgrade(ZombieUpgrade.LEAP_I, UpgradeType.ACTIVE_ABILITY, 5, ZombiePath.GIANT) {
                    icon = createItem(Material.RABBIT_HIDE) {
                        name = "<blue>Leap"
                        description = """
                           <gray>Gives a zombie ability to leap by right clicking on their blade
                        """
                    }

                    level(1.5)
                    level(2.2)

                    action { player, modifier ->
                        val vector = player.eyeLocation.direction.normalize().multiply(modifier)
                        player.velocity = vector
                    }
                }

                upgrade(ZombieUpgrade.VAMPIRE, UpgradeType.MODIFIER, 1, ZombiePath.CAPTAIN) {
                    icon = createItem(Material.QUARTZ_BLOCK) {
                        name = "<blue>Vampire"
                        description = """
                           <gray>Test
                        """
                    }

                    level(1)

                    action { player, modifier ->
                        player.sendMessage("Test $modifier")
                    }
                }

                upgrade(ZombieUpgrade.INFECTION, UpgradeType.PASSIVE_ABILITY, 1) {
                    icon = createItem(Material.POISONOUS_POTATO) {
                        name = "<blue>Infection"
                        description = """
                           <gray>Test2
                        """
                    }

                    level(1)

                    action { player, modifier ->
                        player.sendMessage("infection$modifier")
                    }
                }
            }
        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }


        @EventHandler
        fun onBladeClick(e: PlayerInteractEvent) {
            val player = e.player
            val upgrade = ZombieUpgrade.LEAP_I

            if (!upgrades.hasUpgrade(player, upgrade)) return
            if (KitsManager.getKit(player) !is Zombie) return
            if (e.action != Action.RIGHT_CLICK_AIR) return
            if (e.hand != EquipmentSlot.HAND) return
            if (player.inventory.itemInMainHand.type != Material.WOODEN_SWORD) return

            upgrades.applyAbility(player, upgrade, 0)
        }

        enum class ZombieUpgrade {
            UNDEAD_I,
            UNDEAD_II,
            GIANT_I,
            GIANT_II,
            CAPTAIN_I,
            TOUGH_FLESH_I,
            TOUGH_FLESH_II,
            TOUGH_FLESH_III,
            RAW_DAMAGE_I,
            RAW_DAMAGE_II,
            LEAP_I,
            LEAP_II,
            VAMPIRE,
            INFECTION
        }

        enum class ZombiePath(override val pathName: String) : BasePath {
            UNDEAD("<i><dark_red>Undead"),
            GIANT("<i><dark_green>Giant"),
            CAPTAIN("<i><gold>Captain"),
        }
    }
}