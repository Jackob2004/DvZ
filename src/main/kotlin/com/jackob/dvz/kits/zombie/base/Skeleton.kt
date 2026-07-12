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
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.launchPlayer
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.name
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.withCooldown
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.SkeletonWatcher
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.UUID
import kotlin.random.Random

class Skeleton(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<SkeletonWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.SKELETON) { }

    override val aiZombieEnabled: Boolean = !SkeletonListener.upgrades.hasUpgrade(owner.toPlayer()!!, SkeletonUpgrade.CLOAK_I)

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

        private const val ABRUPT_COOLDOWN = 15

        private val cloakCooldowns = CooldownUtil(CLOAK_COOLDOWN * 1000L)

        private val abruptCooldowns = CooldownUtil(ABRUPT_COOLDOWN * 1000L)

        private val targetsMap = Object2ObjectOpenHashMap<UUID, Pair<UUID, Int>>()

        val upgrades = UpgradesManager.create(SkeletonUpgrade.entries.size, 3, SkeletonPath.entries.size) {
            tier(0) {
                upgrade(SkeletonUpgrade.CLOAK_I, UpgradeType.ACTIVE_ABILITY, 1, SkeletonPath.INFILTRATOR) {
                    icon = createItem(Material.POTION) {
                        name = "<aqua>Cloak"
                        description = """
                             <gray>Gives you invisibility for 5 sec, + 3 sec each upgrade
                             <gray>Cooldown $CLOAK_COOLDOWN sec
                             <green>[Left] <white>click on bow to activate
                             <aqua><u>By starting Infiltrator path you no longer trigger ai zombies
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
                        skeletonPlayer.playSound(
                            skeletonPlayer.location,
                            Sound.ENTITY_WANDERING_TRADER_DRINK_POTION,
                            1f,
                            1f
                        )
                    }

                }
            }

            tier(2) {
                upgrade(SkeletonUpgrade.ABRUPT_MOVEMENTS_I, UpgradeType.ACTIVE_ABILITY, 1, SkeletonPath.INFILTRATOR) {
                    icon = createItem(Material.FEATHER) {
                        name = "<aqua>Abrupt Movements"
                        description = """
                            <gray>Ability that suddenly pushes you forward damaging enemies in line
                            <gray>Dealt damage increases each upgrade
                            <gray>Cooldown $ABRUPT_COOLDOWN sec
                            <green>[Right] <white>click on sword to use it
                        """
                    }

                    (1..3).forEach { level(it) }

                    action { skeletonPlayer, _, modifier ->
                        skeletonPlayer.withCooldown(abruptCooldowns) {
                            val startLocation = location.add(0.0, 1.0, 0.0)
                            sync(delay = TimeUnit.TICKS(1)) {
                                launchPlayer(5.0, 0.0)
                            }

                            val particleBuilder = Particle.CRIT.builder()
                                .offset(0.0, 0.0, 0.0)
                                .count(5)
                                .extra(0.1)

                            val damageEffect = { loc: Location ->
                                loc.getNearbyEntities(1.3, 1.0, 1.3).forEach { entity ->
                                    if (entity is Player && GameManager.getPlayerTeam(entity) == TeamType.DWARF) {
                                        entity.damage(2.0 + modifier, this)
                                    }
                                }
                                particleBuilder.location(loc).receivers(15, true).spawn()
                            }

                            var counter = 4
                            sync(delay = TimeUnit.TICKS(1), period = TimeUnit.TICKS(1)) {

                                counter--
                                if (counter <= 0) {
                                    cancel()
                                    velocity = Vector(0, 0, 0)
                                    val endLocation = location.add(0.0, 1.0, 0.0)
                                    val dirVector = endLocation.clone().subtract(startLocation).toVector().normalize()

                                    var currLoc = startLocation
                                    while (currLoc.distanceSquared(endLocation) > 0.5) {
                                        damageEffect(currLoc)
                                        currLoc.add(dirVector)
                                    }
                                }
                            }

                            playSound(location, Sound.ITEM_SPEAR_HIT, 1f, 1f)
                        }
                    }
                }

                upgrade(SkeletonUpgrade.FIXED_TARGET_I, UpgradeType.PASSIVE_ABILITY, 1, SkeletonPath.SHARPSHOOTER) {
                    icon = createItem(Material.TARGET) {
                        name = "<dark_aqua>Fixed Target"
                        description = """
                            <gray>Increases damage on hitting the same target consecutively
                            <gray>Each upgrade increments max streak by 1
                        """
                    }

                    (1..3).forEach { level(it) }

                    action { skeletonPlayer, dwarfVictim, maxStreak ->
                        val skeletonId = skeletonPlayer.uniqueId
                        val dwarfId = (dwarfVictim as Player).uniqueId

                        val lastTarget: Pair<UUID, Int>? = targetsMap[skeletonId]
                        var streak = 0

                        if (lastTarget == null) {
                            targetsMap[skeletonId] = Pair(dwarfId, streak)
                            return@action
                        }

                        if (lastTarget.first == dwarfId) {
                            streak = if (lastTarget.second + 1 > maxStreak) 0 else lastTarget.second + 1
                        }

                        targetsMap[skeletonId] = Pair(dwarfId, streak)
                        if (streak != 0) {
                            dwarfVictim.damage(4.0 * streak, skeletonPlayer)
                            for (i in 1..streak) {
                                skeletonPlayer.playSound(skeletonPlayer.location, Sound.ENTITY_SKELETON_AMBIENT, 1f, 1f)
                            }
                        }
                    }
                }

                upgrade(SkeletonUpgrade.SKINLESS_I, UpgradeType.MODIFIER, 1) {
                    icon = createItem(Material.BONE_MEAL) {
                        name = "<white>Skinless"
                        description = """
                            <gray>Gives you fire resistance I potion effect
                        """
                    }

                    level(0)

                    action { skeletonPlayer, _, modifier ->
                        val effect = PotionEffect(PotionEffectType.FIRE_RESISTANCE, Int.MAX_VALUE, modifier)
                        skeletonPlayer.addPotionEffect(effect)
                    }
                }
            }
        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        private fun handleSkeletonHitByProjectile(e: ProjectileHitEvent) {
            val playerVictim = e.hitEntity as? Player ?: return
            val shooter = e.entity.shooter as? Player ?: return
            if (GameManager.getPlayerTeam(shooter) != TeamType.DWARF) return

            val upgrade = SkeletonUpgrade.BAG_OF_BONES_I

            if (!upgrades.hasUpgrade(playerVictim, upgrade)) return
            if (KitsManager.getKit(playerVictim) !is Skeleton) return

            upgrades.applyAbility(playerVictim, upgrade, 0)
        }

        private fun handleSkeletonArrowHit(e: ProjectileHitEvent) {
            if (e.entity !is Arrow) return
            val dwarfVictim = e.hitEntity as? Player ?: return
            if (GameManager.getPlayerTeam(dwarfVictim) != TeamType.DWARF) return
            val shooter = e.entity.shooter as? Player ?: return
            if (KitsManager.getKit(shooter) !is Skeleton) return

            val upgrade = SkeletonUpgrade.FIXED_TARGET_I
            if (!upgrades.hasUpgrade(shooter, upgrade)) return

            upgrades.applyAbility(shooter, upgrade, 0, dwarfVictim)
        }

        @EventHandler
        fun onItemClick(e: PlayerInteractEvent) {
            val player = e.player
            if (KitsManager.getKit(player) !is Skeleton) return

            val cloakUpgrade = SkeletonUpgrade.CLOAK_I
            val leftClickedBow = e.leftClickItem?.type == Material.BOW
            if (upgrades.hasUpgrade(player, cloakUpgrade) && leftClickedBow) {
                upgrades.applyAbility(player, cloakUpgrade, 0)
                return
            }

            val abruptUpgrade = SkeletonUpgrade.ABRUPT_MOVEMENTS_I
            val rightClickedSword = e.rightClickItem?.type == Material.COPPER_SWORD
            if (upgrades.hasUpgrade(player, abruptUpgrade) && rightClickedSword) {
                upgrades.applyAbility(player, abruptUpgrade, 0)
            }

        }

        @EventHandler
        fun onProjectileHit(e: ProjectileHitEvent) {
            handleSkeletonHitByProjectile(e)
            handleSkeletonArrowHit(e)
        }

        enum class SkeletonPath(override val pathName: String) : BasePath {
            INFILTRATOR("<i><aqua>Infiltrator"),
            SHARPSHOOTER("<i><dark_aqua>Sharpshooter")
        }
    }
}