package com.jackob.dvz.kits.zombie.base

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.BasePath
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
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
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID

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

        val upgrades = UpgradesManager.create(SkeletonUpgrade.entries.size, 1, SkeletonPath.entries.size) {
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

        enum class SkeletonPath(override val pathName: String) : BasePath {
            INFILTRATOR("<i><aqua>Infiltrator"),
            SHARPSHOOTER("<i><dark_aqua>Sharpshooter")
        }
    }
}