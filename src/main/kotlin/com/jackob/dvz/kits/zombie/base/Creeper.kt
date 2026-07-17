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
import com.jackob.dvz.util.getSphere
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.name
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.withCooldown
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Arrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.HashSet
import java.util.UUID
import kotlin.random.Random

class Creeper(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<CreeperWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CREEPER) { }

    override val aiZombieEnabled: Boolean = true

    private var explosionTask: BukkitTask? = null

    init {
        CreeperListener
    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        startDisguise(player)

        val upgrades = CreeperListener.upgrades
        upgrades.addPlayer(player)
        upgrades.applyModifiers(player)

        val upgrade = CreeperUpgrades.WALL_CRUSHER_I
        if (upgrades.hasUpgrade(player, upgrade)) {
            upgrades.applyAbility(player, upgrade, 0)
        }
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    private fun explode(player: Player, world: World, explosionPower: Float) {
        val location = player.location.add(0.0, 1.0, 0.0)
        world.createExplosion(location, explosionPower, false, true, player)

        val upgrades = CreeperListener.upgrades
        val sheepUpgrade = CreeperUpgrades.SHEEPS_CLOTHING_I
        val chainUpgrade = CreeperUpgrades.CHAIN_REACTION_I

        if (upgrades.hasUpgrade(player, sheepUpgrade)) {
            upgrades.applyAbility(player, sheepUpgrade, 0)
        }

        if (upgrades.hasUpgrade(player, chainUpgrade)) {
            upgrades.applyAbility(player, chainUpgrade, 0)
        }

        player.health = 0.0
    }

    private fun startIgnition(player: Player, explosionPower: Float = 2.5f) {
        if (explosionTask != null) return

        val timeToExplosion = TimeUnit.SECONDS(3)
        val world = player.world

        var timer = timeToExplosion
        explosionTask = sync(period = TimeUnit.TICKS(1)) {
            player.exp = (timer * 100 / timeToExplosion / 100f).coerceIn(0f, 1f)

            timer--
            if (timer <= 0) {
                cancel()
                explosionTask = null
                explode(player, world, explosionPower)
            }
        }

        player.modifyMobDisguise {
            isPowered = true
        }
        world.playSound(player.location, Sound.ENTITY_CREEPER_PRIMED, 1f, 1f)
    }

    private fun stopIgnition(player: Player) {
        if (explosionTask != null) {
            explosionTask!!.cancel()
            explosionTask = null

            player.exp = 0f
            player.modifyMobDisguise {
                isPowered = false
            }
            player.playSound(player.location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f)
        }
    }


    object CreeperListener : Listener {

        private const val WALL_CRUSHER_COOLDOWN = 12

        private val hammer = createItem(Material.IRON_INGOT) {
            name = "<yellow>Wall crusher"
            description = """
                <green>[Right] <white>click on the block to destroy it
                <gray> Cooldown $WALL_CRUSHER_COOLDOWN sec
            """
        }

        private val crusherCooldowns = CooldownUtil(WALL_CRUSHER_COOLDOWN * 1000L)

        val upgrades = UpgradesManager.create(CreeperUpgrades.entries.size, 2, CreeperPath.entries.size) {
            tier(0) {
                upgrade(CreeperUpgrades.SHALLOW_WOUND_I, UpgradeType.PASSIVE_ABILITY, 1, CreeperPath.TRICKSTER) {
                    icon = createItem(Material.ROTTEN_FLESH) {
                        name = "<light_purple>Shallow Wound"
                        description = """
                            <gray>Gives you 20% chance to get regeneration effect after being hit by an arrow
                            <gray>+5% each upgrade
                        """
                    }

                    (20..35 step 5).forEach { level(it) }

                    action { creeperPlayer, _, chance ->
                        if (Random.nextInt(100) >= chance) return@action

                        val regenEffect = PotionEffect(PotionEffectType.REGENERATION, 3 * 20, 1)
                        creeperPlayer.addPotionEffect(regenEffect)
                    }
                }

                upgrade(CreeperUpgrades.WALL_CRUSHER_I, UpgradeType.ACTIVE_ABILITY, 1, CreeperPath.DEMOLISHER) {
                    icon = createItem(Material.IRON_INGOT) {
                        name = "<green>Wall Crusher"
                        description = """
                            <gray>Gives you a hammer that can break blocks instantly
                            <gray>Cooldown $WALL_CRUSHER_COOLDOWN sec
                            <gray>Each upgrade gives you +10% chance to cancel cooldown after usage
                        """
                    }

                    (10..30 step 10).forEach { level(it) }

                    action { creeperPlayer, _, _ ->
                        creeperPlayer.inventory.addItem(hammer)
                    }

                    action { creeperPlayer, _, chance ->
                        if (Random.nextInt(100) >= chance) return@action
                        crusherCooldowns.removeFromCooldown(creeperPlayer)
                        creeperPlayer.playSound(creeperPlayer.location, Sound.ENTITY_CREEPER_HURT, 1f, 1f)
                    }
                }

                upgrade(CreeperUpgrades.HARDENING_I, UpgradeType.MODIFIER, 1) {
                    icon = createItem(Material.SHIELD) {
                        name = "<white>Hardening"
                        description = """
                            <gray>Gives you resistance I effect
                        """
                    }

                    level(0)

                    action { creeperPlayer, _, modifier ->
                        val resistanceEffect =
                            PotionEffect(PotionEffectType.RESISTANCE, Int.MAX_VALUE, modifier, false, false)
                        creeperPlayer.addPotionEffect(resistanceEffect)
                    }
                }

            }

            tier(1) {
                upgrade(CreeperUpgrades.SHEEPS_CLOTHING_I, UpgradeType.PASSIVE_ABILITY, 1, CreeperPath.TRICKSTER) {
                    icon = createItem(Material.WHITE_WOOL) {
                        name = "<light_purple>Sheep's Clothing"
                        description = """
                            <gray>Your explosion now turn half of the nearby blocks into wool
                            <gray>Range of the effect increases each upgrade 
                        """
                    }

                    (4..6).forEach { level(it) }

                    @Suppress("UnstableApiUsage")
                    action { creeperPlayer, _, effectRadius ->
                        val replaceMaterial = Material.PINK_WOOL
                        val nearbyBlocksLocations = creeperPlayer.location.getSphere(effectRadius, false)

                        var canBreak = true
                        for (l in nearbyBlocksLocations) {
                            val block = l.block
                            val blockType = block.type
                            if (blockType== Material.AIR || blockType  == replaceMaterial) continue

                            val blockBreak = BlockBreakEvent(block, creeperPlayer)
                            Bukkit.getPluginManager().callEvent(blockBreak)

                            if (!blockBreak.isCancelled && canBreak) {
                                block.type = replaceMaterial
                            }
                            canBreak = !canBreak
                        }
                    }
                }

                upgrade(CreeperUpgrades.CHAIN_REACTION_I, UpgradeType.PASSIVE_ABILITY, 1, CreeperPath.DEMOLISHER) {
                    icon = createItem(Material.TNT_MINECART) {
                        name = "<green>Chain Reaction"
                        description = """
                            <gray>Smaller explosions will hit nearby dwarfs in a chain reaction
                            <gray>Each level max reactions value is increased starting from 1
                        """
                    }

                    (1..3).forEach { level(it) }

                    action { creeperPlayer, _, maxReactions ->
                        val effectRange = 6.0
                        val world = creeperPlayer.world
                        val victims = HashSet<UUID>(maxReactions)

                        fun playChainReaction(reactions: Int, rootLocation: Location?) {
                            if (reactions <= 0 || rootLocation == null) return

                            var currReactions = reactions - 1
                            var newRootLocation: Location? = null

                            val entitiesInRange = rootLocation.getNearbyEntities(effectRange, effectRange , effectRange)
                            for (e in entitiesInRange) {
                                val victim = e as? Player ?: continue
                                val id = victim.uniqueId

                                if (victims.contains(id)) continue
                                if (GameManager.getPlayerTeam(victim) != TeamType.DWARF) continue

                                newRootLocation = victim.location
                                victims.add(id)
                                world.createExplosion(creeperPlayer, newRootLocation, 1f, false, false)
                                break
                            }

                            sync(delay = TimeUnit.TICKS(12)) {
                                playChainReaction(currReactions, newRootLocation)
                            }
                        }

                        playChainReaction(maxReactions, creeperPlayer.location)
                    }
                }

                upgrade(CreeperUpgrades.NITROGLYCERIN_I, UpgradeType.PASSIVE_ABILITY, 1) {
                    icon = createItem(Material.TNT) {
                        name = "<white>Nitroglycerin"
                        description = """
                            <gray>Makes your explosion power stronger each level
                        """
                    }

                    level(3f)
                    level(3.5f)
                    level(4f)

                    action { creeperPlayer, _, modifier ->
                        val creeperKit = KitsManager.getKit(creeperPlayer) as Creeper
                        creeperKit.startIgnition(creeperPlayer, modifier)
                    }
                }

            }
        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @Suppress("UnstableApiUsage")
        private fun destroyBlock(player: Player, block: Block) = player.withCooldown(crusherCooldowns) {
            val blockBreak = BlockBreakEvent(block, this)
            Bukkit.getPluginManager().callEvent(blockBreak)

            if (!blockBreak.isCancelled) {
                block.breakNaturally(true)
            }

            world.spawnParticle(Particle.EXPLOSION, block.location, 1)
            playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
            upgrades.applyAbility(player, CreeperUpgrades.WALL_CRUSHER_I, 1)
        }

        @EventHandler
        fun onGunpowderClick(e: PlayerInteractEvent) {
            val player = e.player
            val creeperKit = KitsManager.getKit(player) as? Creeper ?: return

            val rightItem = e.rightClickItem
            val upgrade = CreeperUpgrades.NITROGLYCERIN_I

            if (rightItem?.type == Material.GUNPOWDER) {
                if (upgrades.hasUpgrade(player, upgrade)) {
                    upgrades.applyAbility(player, upgrade, 0)
                } else {
                    creeperKit.startIgnition(player)
                }
            } else if (e.leftClickItem?.type == Material.GUNPOWDER) {
                creeperKit.stopIgnition(player)
            } else if (rightItem?.type == hammer.type) {
                e.clickedBlock?.let { destroyBlock(player, it) }
            }

        }

        @EventHandler
        fun onArrowDamage(e: ProjectileHitEvent) {
            if (e.entity !is Arrow) return
            val creeperVictim = e.hitEntity as? Player ?: return
            if (KitsManager.getKit(creeperVictim) !is Creeper) return

            val attacker = e.entity.shooter as? Player ?: return
            if (GameManager.getPlayerTeam(attacker) != TeamType.DWARF) return

            val upgrade = CreeperUpgrades.SHALLOW_WOUND_I
            if (!upgrades.hasUpgrade(creeperVictim, upgrade)) return

            upgrades.applyAbility(creeperVictim, upgrade, 0)
        }

        enum class CreeperPath(override val pathName: String) : BasePath {
            TRICKSTER("<i><light_purple>Trickster"),
            DEMOLISHER("<i><green>Demolisher")
        }
    }
}
