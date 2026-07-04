package com.jackob.dvz.kits.zombie.base

import com.destroystokyo.paper.ParticleBuilder
import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.AIZombieSpawnEvent
import com.jackob.dvz.core.events.ZombieDeathEvent
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.kits.*
import com.jackob.dvz.util.*
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
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
import org.joml.Matrix4f
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
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

        val upgrades = ZombieListener.upgrades
        upgrades.addPlayer(player)
        upgrades.applyModifiers(player)

        val graveUpgrade = ZombieUpgrade.GRAVEYARD_I
        val cryUpgrade = ZombieUpgrade.DESPERATE_CRY_I
        if (upgrades.hasUpgrade(player, graveUpgrade)) {
            upgrades.applyAbility(player, graveUpgrade, 1)
        } else if (upgrades.hasUpgrade(player, cryUpgrade)) {
            upgrades.applyAbility(player, cryUpgrade, 1)
        }
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!
        stopDisguise(player)

        val upgrades = ZombieListener.upgrades
        val upgrade = ZombieUpgrade.DESPERATE_CRY_I
        if (upgrades.hasUpgrade(player, upgrade)) {
            upgrades.applyAbility(player, upgrade, 2)
        }
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

        private const val HAMMER_COOLDOWN = 13

        private const val DEATH_INTERVAL = 5

        private const val CRY_DISTANCE = 10

        private const val MIN_ZOMBIE_DEATHS = 5

        private val leapMap = CooldownUtil(LEAP_COOLDOWN * 1000L)

        private val bannerMap = CooldownUtil(BANNER_COOLDOWN * 1000L)

        private val hammerMap = CooldownUtil(HAMMER_COOLDOWN * 1000L)

        private val rebirthMap = Int2LongOpenHashMap()

        private val rebirthVisualMap = Int2ObjectOpenHashMap<UUID>()

        private val deathInterval = CooldownUtil(DEATH_INTERVAL * 1000L)

        private val deathsInInterval = Object2IntLinkedOpenHashMap<UUID>()

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

                upgrade(ZombieUpgrade.SLEDGEHAMMER_I, UpgradeType.ACTIVE_ABILITY, 1, ZombiePath.GIANT) {
                    icon = createItem(Material.MACE) {
                        name = "<white>Sledgehammer"
                        description = """
                           <gray>Gives you ability to create a hammer in front of you.
                           <gray>It falls down knocking enemies off you and dealing damage.
                           <gray>Right click on blade to activate it.
                           <gray>Gets stronger each level.
                           <gray>Cooldown ($HAMMER_COOLDOWN s)
                        """
                    }

                    (2..6 step 2).forEach { level(it) }

                    action { zombiePlayer, _, modifier ->
                        if (hammerMap.isOnCooldown(zombiePlayer)) {
                            displayCooldown(zombiePlayer, hammerMap)
                        } else {
                            playHammerEffect(zombiePlayer, modifier.toDouble())
                        }
                    }
                }

                upgrade(ZombieUpgrade.DESPERATE_CRY_I, UpgradeType.PASSIVE_ABILITY, 1, ZombiePath.CAPTAIN) {
                    icon = createItem(Material.GHAST_TEAR) {
                        name = "<white>Desperate cry"
                        description = """
                           <gray>Gives you buff of strength and speed for 5 sec.
                           <gray>It is triggered by $MIN_ZOMBIE_DEATHS zombies dying in the range.
                           <gray>of $CRY_DISTANCE blocks near you in duration of $DEATH_INTERVAL seconds.
                        """
                    }

                    (5..7).forEach { level(it) }
                    // on ally zombie death
                    action { zombiePlayer, deadZombieAlly, modifier ->
                        val distance = zombiePlayer.location.distanceSquared(deadZombieAlly!!.location)
                        if (distance > CRY_DISTANCE * CRY_DISTANCE) return@action

                        val playerId = zombiePlayer.uniqueId
                        if (deathInterval.isOnCooldownSafe(zombiePlayer)) {
                            val deaths = deathsInInterval.getInt(playerId) + 1
                            if (deaths >= MIN_ZOMBIE_DEATHS) {
                                deathsInInterval.put(playerId, 0)
                                deathInterval.removeFromCooldown(zombiePlayer)

                                val duration = modifier * 20
                                zombiePlayer.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, duration, 1))
                                zombiePlayer.addPotionEffect(PotionEffect(PotionEffectType.SPEED, duration, 1))
                                zombiePlayer.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, duration, 1))
                                zombiePlayer.playSound(zombiePlayer.location, Sound.ENTITY_ZOMBIE_HURT, 1.0f, 1.0f)
                            } else {
                                deathsInInterval.put(playerId, deaths)
                            }

                        } else {
                            deathInterval.putOnCooldown(zombiePlayer)
                            deathsInInterval.put(playerId, 1)
                        }
                    }
                    // on activate
                    action { zombiePlayer, _, _ ->
                        deathsInInterval.put(zombiePlayer.uniqueId, 0)
                    }
                    // on deactivate
                    action { zombiePlayer, _, _ ->
                        deathInterval.removeFromCooldown(zombiePlayer)
                        deathsInInterval.removeInt(zombiePlayer.uniqueId)
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

        private fun playHammerEffect(player: Player, modifier: Double) {
            val directionVector = player.location.direction.normalize().multiply(3)
            val displayLocation = player.location.add(directionVector).add(0.0, 2.0, 0.0)
            val groundLocation = displayLocation.clone().subtract(0.0, 2.0, 0.0)

            val display = player.world.spawn(displayLocation, ItemDisplay::class.java) { consumer ->
                consumer.setItemStack(ItemStack(Material.MACE))
                consumer.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.FIXED
            }
            player.playSound(player.location, Sound.ITEM_MACE_SMASH_AIR, 1f, 1f)

            val degreesInRadians = Math.toRadians(120.0).toFloat()
            sync(delay = TimeUnit.SECONDS(1)) {
                display.interpolationDuration = 20
                display.interpolationDelay = 0

                val horizontalMatrix = Matrix4f().rotateZ(degreesInRadians)
                display.setTransformationMatrix(horizontalMatrix)
            }

            sync(delay = TimeUnit.SECONDS(3)) {
                display.interpolationDuration = 7
                display.interpolationDelay = 0

                val horizontalMatrix = Matrix4f().translate(0f, -4f, 0f).rotateZ(degreesInRadians)
                display.setTransformationMatrix(horizontalMatrix)
            }

            sync(delay = TimeUnit.TICKS(65)) {
                val blockData = Material.STONE.createBlockData()
                Particle.BLOCK.builder()
                    .location(groundLocation)
                    .data(blockData)
                    .offset(0.3, 0.1, 0.3)
                    .count(40)
                    .extra(0.1)
                    .receivers(15, true)
                    .spawn()

                groundLocation.getWorld().playSound(groundLocation, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f)

                val range = 3.0
                directionVector.y = 0.1
                for (e in groundLocation.getNearbyEntities(range, range, range)) {
                    if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) {
                        e.velocity = directionVector
                        e.damage(modifier, player)
                    }
                }
                display.remove()
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
            } else if (upgrades.hasUpgrade(player, ZombieUpgrade.SLEDGEHAMMER_I)) {
                upgrades.applyAbility(player, ZombieUpgrade.SLEDGEHAMMER_I, 0)
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

        @EventHandler
        fun onZombieDeath(e: ZombieDeathEvent) {
            for (id in deathsInInterval.keys) {
                val player = Bukkit.getPlayer(id) ?: continue
                upgrades.applyAbility(player, ZombieUpgrade.DESPERATE_CRY_I, 0, e.victim)
            }
        }

        enum class ZombiePath(override val pathName: String) : BasePath {
            UNDEAD("<i><dark_red>Undead"),
            GIANT("<i><dark_green>Giant"),
            CAPTAIN("<i><gold>Captain"),
        }

        data class BigBoyData(val knockbackResistance: Double, val scale: Double, val speed: Double, val jump: Double)
    }
}