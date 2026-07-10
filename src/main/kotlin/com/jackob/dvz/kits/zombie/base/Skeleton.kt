package com.jackob.dvz.kits.zombie.base

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.BasePath
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.kits.UpgradeType
import com.jackob.dvz.kits.UpgradesManager
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.name
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.withCooldown
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.SkeletonWatcher
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.random.Random

class Skeleton(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<SkeletonWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.SKELETON) { }

    override val aiZombieEnabled: Boolean = true

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        startDisguise(player)

        val upgrades = SkeletonListener.upgrades
        upgrades.addPlayer(player)
        upgrades.applyModifiers(player)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    init {
        SkeletonListener
    }

    object SkeletonListener : Listener {

        private const val CLOAK_COOLDOWN = 20

        private val cloakCooldowns = CooldownUtil(20 * 1000)

        val upgrades = UpgradesManager.create(SkeletonUpgrade.entries.size, 2, SkeletonPath.entries.size) {
            tier(0) {
                upgrade(SkeletonUpgrade.CLOAK_I, UpgradeType.ACTIVE_ABILITY, 1, SkeletonPath.INFILTRATOR) {
                    icon = createItem(Material.POTION) {
                        name = "<aqua>Cloak"
                        description = """
                             <gray>Gives you invisibility for 5 sec, + 3 sec each upgrade
                             <gray>Cooldown $CLOAK_COOLDOWN sec
                             <green>[Left] <white>click on bow to activate
                        """
                    }

                    (5..12 step 3).forEach { level(it) }

                    action { skeletonPlayer, _, duration ->
                        skeletonPlayer.withCooldown(cloakCooldowns) {
                            addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0))
                            playSound(location, Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 1f, 1f)
                        }
                    }
                }

                upgrade(SkeletonUpgrade.ARMORY_I, UpgradeType.MODIFIER, 1, SkeletonPath.SHARPSHOOTER) {
                    icon = createItem(Material.CHAINMAIL_CHESTPLATE) {
                        name = "<dark_aqua>Armory"
                        description = """
                             <gray>Gives you chainmail plate with protection 1
                             <gray>Each upgrade you get + 1 projectile protection
                        """
                    }

                    (1..3).forEach { level(it) }

                    action { skeletonPlayer, _, enchantmentLevel ->
                        val plate = createItem(Material.CHAINMAIL_CHESTPLATE) {
                            name = "<gray>Dusty plate"
                            enchant(Enchantment.PROJECTILE_PROTECTION, enchantmentLevel)
                            enchant(Enchantment.PROTECTION, 1)
                        }

                        skeletonPlayer.inventory.addItem(plate)
                    }
                }

                upgrade(SkeletonUpgrade.LEATHER_QUIVER_I, UpgradeType.MODIFIER, 1) {
                    icon = createItem(Material.ARROW) {
                        name = "<white>Leather Quiver"
                        description = """
                            <gray>Gives you + 16 arrows each upgrade
                        """
                    }

                    (1..4).forEach { level(it) }

                    action { skeletonPlayer, _, multiplier ->
                        val baseArrows = 16
                        skeletonPlayer.inventory.addItem(createItem(Material.ARROW, baseArrows * multiplier) {})
                    }

                }

            }

            tier(1) {
                upgrade(SkeletonUpgrade.SLIM_BLADE_I, UpgradeType.MODIFIER, 1, SkeletonPath.INFILTRATOR) {
                    icon = createItem(Material.COPPER_SWORD) {
                        name = "<aqua>Slim Blade"
                        description = """
                            <gray>Gives you a copper sword with sharpness I
                            <gray>Holding it gives you speed buff
                            <gray>Both sharpness and speed increase each upgrade
                        """
                    }

                    level(1)
                    level(2)

                    action { skeletonPlayer, _, multiplier ->
                        val attr = Attribute.MOVEMENT_SPEED
                        val modifier =
                            AttributeModifier(attr.key, 0.04 * multiplier, AttributeModifier.Operation.ADD_NUMBER)
                        val blade = createItem(Material.COPPER_SWORD) {
                            name = "<gray>Slim Blade"
                            enchant(Enchantment.SHARPNESS, multiplier)
                            addAttributeModifier(attr, modifier)
                        }

                        skeletonPlayer.inventory.addItem(blade)
                    }

                }

                upgrade(SkeletonUpgrade.POISONED_ARROW_I, UpgradeType.MODIFIER, 1, SkeletonPath.SHARPSHOOTER) {
                    icon = createItem(Material.TIPPED_ARROW) {
                        name = "<dark_aqua>Poisoned Arrow"
                        description = """
                            <gray>Gives you 16x wither arrows
                            <gray>+ 8 each upgrade
                        """
                    }

                    level(16)
                    level(24)

                    action { skeletonPlayer, _, amountOfArrows ->
                        val effect = PotionEffect(PotionEffectType.WITHER, 8 * 20 * 8, 0)
                        val arrows = createItem<PotionMeta>(Material.TIPPED_ARROW, amountOfArrows) {
                            name = "<dark_green>Dipped in wither's blood"
                            addCustomEffect(effect, true)
                            color = Color.GREEN
                        }

                        skeletonPlayer.inventory.addItem(arrows)
                    }

                }

                upgrade(SkeletonUpgrade.BAG_OF_BONES_I, UpgradeType.PASSIVE_ABILITY, 1) {
                    icon = createItem(Material.BONE) {
                        name = "<white>Bag Of Bones"
                        description = """
                            <gray>Gives you 10% chance to gain instant health on any projectile hit
                            <gray>+ 10% each upgrade
                        """
                    }

                    (10..40 step 10).forEach { level(it) }

                    action { skeletonPlayer, _, chance ->
                        if (Random.nextInt(100) >= chance) return@action
                        val effect = PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0)
                        skeletonPlayer.addPotionEffect(effect)
                        skeletonPlayer.playSound(skeletonPlayer.location, Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, 1f, 1f)
                    }

                }

            }
        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onBowClick(e: PlayerInteractEvent) {
            val player = e.player
            val upgrade = SkeletonUpgrade.CLOAK_I

            if (!upgrades.hasUpgrade(player, upgrade)) return
            if (KitsManager.getKit(player) !is Skeleton) return
            val item = e.leftClickItem ?: return
            if (item.type != Material.BOW) return

            upgrades.applyAbility(player, upgrade, 0)
        }

        @EventHandler
        fun onProjectileHit(e: ProjectileHitEvent) {
            val playerVictim = e.hitEntity as? Player ?: return
            val shooter = e.entity.shooter as? Player ?: return
            if (GameManager.getPlayerTeam(shooter) != TeamType.DWARF) return

            val upgrade = SkeletonUpgrade.BAG_OF_BONES_I

            if (!upgrades.hasUpgrade(playerVictim, upgrade)) return
            if (KitsManager.getKit(playerVictim) !is Skeleton) return

            upgrades.applyAbility(playerVictim, upgrade, 0)
        }

        enum class SkeletonPath(override val pathName: String) : BasePath {
            INFILTRATOR("<i><aqua>Infiltrator"),
            SHARPSHOOTER("<i><dark_aqua>Sharpshooter")
        }
    }
}