package com.jackob.dvz.kits.zombie.base

import com.destroystokyo.paper.ParticleBuilder
import com.jackob.dvz.DvZ
import com.jackob.dvz.core.events.AIZombieSpawnEvent
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.kits.*
import com.jackob.dvz.util.*
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


class Zombie(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.ZOMBIE) { }

    override val aiZombieEnabled: Boolean = false

    init {
        ZombieListener
    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        startDisguise(player)

        val upgrades = ZombieListener.upgrades
        upgrades.addPlayer(player)
        upgrades.applyModifiers(player)

        val upgrade = ZombieListener.ZombieUpgrade.GRAVEYARD_I
        if (upgrades.hasUpgrade(player, upgrade)) {
            upgrades.applyAbility(player, upgrade, 1)
        }
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    private fun spinOn(player: Player) {
        player.modifyMobDisguise {
            isSpinning = true
        }
    }

    private fun spinOff(player: Player) {
        player.modifyMobDisguise {
            isSpinning = false
        }
    }

    private fun applyBigBoy(player: Player, updatedScale: Double) {
        player.modifyMobDisguise {
            isSneaking = true
            scale = updatedScale
        }
    }

    object ZombieListener : Listener {

        private const val LEAP_COOLDOWN = 15

        private const val BANNER_COOLDOWN = 16

        private val leapMap = CooldownUtil(LEAP_COOLDOWN * 1000L)

        private val bannerMap = CooldownUtil(BANNER_COOLDOWN * 1000L)

        private val rebirthMap = Int2LongOpenHashMap()

        private val rebirthVisualMap = Int2ObjectOpenHashMap<UUID>()

        private val bannerItem = ItemStack(Material.GREEN_BANNER).apply {
            editMeta(BannerMeta::class.java) {
                it.addPattern(Pattern(DyeColor.BLACK, PatternType.SKULL))
            }
        }

        private val rebirthItem = createItem(Material.RED_CANDLE) {
            name = "<dark_red>Rebirth"
            description = """
                  <gray>Click to be reborn in the place you died.
            """
        }

        val upgrades = UpgradesManager.create(ZombieUpgrade.entries.size, 3, ZombiePath.entries.size) {
            tier(0) {
                upgrade(ZombieUpgrade.INFECTION_I, UpgradeType.PASSIVE_ABILITY, 1, ZombiePath.UNDEAD) {
                    icon = createItem(Material.POISONOUS_POTATO) {
                        name = "<dark_purple>Infection"
                        description = """
                           <gray>Gives you 2% chance of giving dwarf poison effect I on hitting him.
                           <gray>Effect lasts 6 seconds.
                           <gray>Chance increases each level by 2%.
                        """
                    }

                    (2..6 step 2).forEach { level(it) }

                    action { _, dwarfVictim, chance ->
                        if (Random.nextInt(100) >= chance) return@action
                        dwarfVictim!!.addPotionEffect(PotionEffect(PotionEffectType.POISON, 6 * 20, 0))
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

                    action { zombiePlayer, _, modifier ->
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

                    action { zombieEntity, _, modifier ->
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

                    action { zombiePlayer, _, modifier ->
                        val attr = zombiePlayer.getAttribute(Attribute.ATTACK_DAMAGE) ?: return@action
                        attr.baseValue += modifier
                    }
                }

            }

            tier(1) {
                upgrade(ZombieUpgrade.LEAP_I, UpgradeType.ACTIVE_ABILITY, 1, ZombiePath.UNDEAD) {
                    icon = createItem(Material.RABBIT_FOOT) {
                        name = "<gold>Leap"
                        description = """
                           <gray>Launches you forward and upward.
                           <gray>[Right] click on you blade to activate.
                           <gray>Each level ability gets stronger.
                           <gray>Cooldown ($LEAP_COOLDOWN sec)
                        """
                    }


                    level(1.8)
                    level(2.2)

                    action { zombiePlayer, _, modifier ->

                        if (leapMap.isOnCooldown(zombiePlayer)) {
                            displayCooldown(zombiePlayer, leapMap)
                        } else {
                            val vector = zombiePlayer.location.direction.normalize().multiply(modifier)
                            vector.y = 0.7
                            zombiePlayer.velocity = vector

                            (KitsManager.getKit(zombiePlayer) as Zombie).spinOn(zombiePlayer)

                            sync(delay = TimeUnit.SECONDS(3)) {
                                (KitsManager.getKit(zombiePlayer) as? Zombie)?.spinOff(zombiePlayer)
                            }

                            zombiePlayer.playSound(zombiePlayer.location, Sound.ENTITY_BREEZE_WIND_BURST, 1f, 1f)
                        }
                    }
                }

                upgrade(ZombieUpgrade.BIG_BOY_I, UpgradeType.MODIFIER, 1, ZombiePath.GIANT) {
                    icon = createItem(Material.IRON_CHESTPLATE) {
                        name = "<gold>Big boy"
                        description = """
                           <gray>Makes you bigger and slower but stronger
                           <gray>also gives you knockback resistance.
                           <gray>Each upgrade effect increases except for the strength potion effect.
                        """
                    }

                    level(BigBoyData(0.3, 1.2, 0.09, 0.5))
                    level(BigBoyData(0.6, 1.4, 0.08, 0.6))

                    action { zombiePlayer, _, modifier ->
                        zombiePlayer.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, Int.MAX_VALUE, 0))
                        zombiePlayer.getAttribute(Attribute.GRAVITY)?.baseValue = 0.04
                        zombiePlayer.getAttribute(Attribute.KNOCKBACK_RESISTANCE)?.baseValue =
                            modifier.knockbackResistance
                        zombiePlayer.getAttribute(Attribute.SCALE)?.baseValue = modifier.scale
                        zombiePlayer.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = modifier.speed
                        zombiePlayer.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue = modifier.jump

                        (KitsManager.getKit(zombiePlayer) as Zombie).applyBigBoy(zombiePlayer, modifier.scale)
                    }
                }

                upgrade(ZombieUpgrade.BANNER_CARRIER_I, UpgradeType.ACTIVE_ABILITY, 1, ZombiePath.CAPTAIN) {
                    icon = createItem(Material.GREEN_BANNER) {
                        name = "<gold>Banner carrier"
                        description = """
                           <gray>Gives you ability to spawn banner that
                           <gray>will give strength buff to nearby zombies.
                           <gray>Lasts 7 sec increases by 3 every level.
                           <gray>Cooldown ($BANNER_COOLDOWN sec)
                        """
                    }

                    level(7)
                    level(10)

                    action { zombiePlayer, _, modifier ->
                        if (bannerMap.isOnCooldown(zombiePlayer)) {
                            displayCooldown(zombiePlayer, bannerMap)
                            return@action
                        }

                        val spawnLoc = zombiePlayer.location.add(0.0, 1.0, 0.0)
                        spawnLoc.pitch = 0.0f
                        val bannerDisplay =
                            zombiePlayer.world.spawn(spawnLoc, ItemDisplay::class.java) { consumer ->
                                consumer.setItemStack(bannerItem)
                            }

                        val potionEffect = PotionEffect(PotionEffectType.STRENGTH, 2 * 20, 0)
                        val particleEffectLoc = spawnLoc.subtract(0.0, 1.0, 0.0)
                        val particleBuilder: ParticleBuilder = Particle.DUST.builder()
                            .color(Color.RED, 1.0f)

                        var timer: Int = modifier

                        sync(period = TimeUnit.SECONDS(1)) {
                            for (t in spawnLoc.getNearbyEntities(10.0, 10.0, 10.0)) {
                                if (t == zombiePlayer) continue
                                if ((t is Player && KitsManager.getKit(t) is Zombie) || t.type == AIZombieScheduler.MOB_TYPE) {
                                    (t as LivingEntity).addPotionEffect(potionEffect)
                                }
                            }
                            playCircleEffect(particleBuilder, particleEffectLoc)

                            timer--
                            if (timer <= 0) {
                                cancel()
                                bannerDisplay.remove()
                            }
                        }
                        zombiePlayer.playSound(spawnLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 1f)
                    }
                }

                upgrade(ZombieUpgrade.TOUGH_FLESH_I, UpgradeType.MODIFIER, 1) {
                    icon = createItem(Material.ROTTEN_FLESH) {
                        name = "<gold>Tough flesh"
                        description = """
                           <gray>Gives you two extra hearts each upgrade.
                        """
                    }

                    (4..16 step 4).forEach { level(it) }

                    action { zombiePlayer, _, modifier ->
                        val attr = zombiePlayer.getAttribute(Attribute.MAX_HEALTH) ?: return@action
                        attr.baseValue += modifier
                    }
                }
            }

            tier(2) {
                upgrade(ZombieUpgrade.GRAVEYARD_I, UpgradeType.PASSIVE_ABILITY, 1, ZombiePath.UNDEAD) {
                    icon = createItem(Material.RED_CANDLE) {
                        name = "<white>Graveyard"
                        description = """
                           <gray>Gives you 20% chance to respawn in the place you died.
                           <gray>Can be activated by clicking candle in your inventory.
                           <gray>Chance increases by 10% each level.
                           <gray>You have 6 sec after death to activate it.
                        """
                    }

                    (20..40 step 10).forEach { level(it) }
                    // on death
                    action { zombiePlayer, _, chance ->
                        if (Random.nextInt(100) >= chance) return@action
                        val location = zombiePlayer.location
                        val playerId = zombiePlayer.entityId

                        rebirthMap.put(playerId, location.packCoordinates())

                        val disguise = MobDisguise(DisguiseType.ZOMBIE)
                        val watcher = disguise.watcher
                        watcher.isSleeping = true

                        DisguiseAPI.disguiseNextEntity(disguise)
                        val dummy = location.world.spawnEntity(location, EntityType.ITEM_DISPLAY)
                        rebirthVisualMap[playerId] = dummy.uniqueId

                        sync(delay = TimeUnit.SECONDS(6)) {
                            cancelRebirth(zombiePlayer)
                        }
                    }
                    // on kit activate
                    action { zombiePlayer, _, _ ->
                        val id = zombiePlayer.entityId
                        if (rebirthMap.contains(id)) {
                            zombiePlayer.inventory.addItem(rebirthItem)
                        }
                    }
                    // on item click
                    action { zombiePlayer, _, _ ->
                        val playerId = zombiePlayer.entityId
                        val loc = rebirthMap.remove(playerId).unpackToLocation(zombiePlayer.world)

                        zombiePlayer.teleport(loc)
                        zombiePlayer.removeItem(rebirthItem, 1)
                        Bukkit.getEntity(rebirthVisualMap.remove(playerId))?.remove()
                    }

                }

                upgrade(ZombieUpgrade.MINER_I, UpgradeType.MODIFIER, 1) {
                    icon = createItem(Material.GOLDEN_PICKAXE) {
                        name = "<white>Miner"
                        description = """
                           <gray>Gives you golden pickaxe
                        """
                    }

                    level(0)

                    action { zombiePlayer, _, _ ->
                        zombiePlayer.inventory.addItem(ItemStack(Material.GOLDEN_PICKAXE))
                    }
                }
            }

        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        private fun displayCooldown(player: Player, map: CooldownUtil) {
            val time = map.getRemainingTime(player)!!.toSeconds().toInt()
            player.sendActionBar("<gold>Wait <gray>${time}s <gold>to use the ability".mm())
        }

        private fun playCircleEffect(effect: ParticleBuilder, loc: Location) {
            val radius = 3
            for (degree in 0..359) {
                val radians = Math.toRadians(degree.toDouble())
                val x = cos(radians) * radius
                val z = sin(radians) * radius
                loc.add(x, 0.0, z)
                effect.location(loc).receivers(12, true).spawn()
                loc.subtract(x, 0.0, z)
            }
        }

        private fun cancelRebirth(player: Player) {
            val id = player.entityId
            if (rebirthMap.containsKey(id)) {
                player.removeItem(rebirthItem, 1)
                rebirthMap.remove(id)
                Bukkit.getEntity(rebirthVisualMap.remove(id))?.remove()
            }
        }

        @EventHandler
        fun onZombieDealDamage(e: EntityDamageByEntityEvent) {
            val attacker = e.damager as? Player ?: return
            val upgrade = ZombieUpgrade.INFECTION_I
            if (!upgrades.hasUpgrade(attacker, upgrade)) return
            if (KitsManager.getKit(attacker) !is Zombie) return

            val dwarfVictim = e.entity as? Player ?: return

            upgrades.applyAbility(attacker, upgrade, 0, dwarfVictim)
        }

        @EventHandler
        fun onAIZombieSpawn(e: AIZombieSpawnEvent) {
            val player = e.zombiePlayer
            val upgrade = ZombieUpgrade.LEADERSHIP_I
            if (!upgrades.hasUpgrade(player, upgrade)) return
            if (KitsManager.getKit(player) !is Zombie) return

            for (i in 0..(e.zombies.size / 2).minus(1)) {
                upgrades.applyAbility(player, upgrade, 0, e.zombies[i])
            }
        }

        @EventHandler
        fun onBladeClick(e: PlayerInteractEvent) {
            val player = e.player

            if (KitsManager.getKit(player) !is Zombie) return
            val item = e.rightClickItem ?: return

            if (item == rebirthItem) {
                upgrades.applyAbility(player, ZombieUpgrade.GRAVEYARD_I, 2)
                return
            }

            if (item.type != Material.WOODEN_SWORD) return

            if (upgrades.hasUpgrade(player, ZombieUpgrade.LEAP_I)) {
                upgrades.applyAbility(player, ZombieUpgrade.LEAP_I, 0)
            } else if (upgrades.hasUpgrade(player, ZombieUpgrade.BANNER_CARRIER_I)) {
                upgrades.applyAbility(player, ZombieUpgrade.BANNER_CARRIER_I, 0)
            }

        }

        @EventHandler(priority = EventPriority.LOWEST)
        fun onPlayerDeath(e: PlayerDeathEvent) {
            val player = e.player
            val upgrade = ZombieUpgrade.GRAVEYARD_I
            if (!upgrades.hasUpgrade(player, upgrade)) return
            if (KitsManager.getKit(player) !is Zombie) return

            upgrades.applyAbility(player, upgrade, 0)
        }

        @EventHandler
        fun onPlayerLeave(e: PlayerQuitEvent) {
            cancelRebirth(e.player)
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
            LEAP_I,
            LEAP_II,
            BIG_BOY_I,
            BIG_BOY_II,
            BANNER_CARRIER_I,
            BANNER_CARRIER_II,
            TOUGH_FLESH_I,
            TOUGH_FLESH_II,
            TOUGH_FLESH_III,
            TOUGH_FLESH_IV,
            GRAVEYARD_I,
            GRAVEYARD_II,
            GRAVEYARD_III,
            MINER_I,
        }

        enum class ZombiePath(override val pathName: String) : BasePath {
            UNDEAD("<i><dark_red>Undead"),
            GIANT("<i><dark_green>Giant"),
            CAPTAIN("<i><gold>Captain"),
        }

        data class BigBoyData(val knockbackResistance: Double, val scale: Double, val speed: Double, val jump: Double)
    }
}