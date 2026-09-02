package com.jackob.dvz.kits.dwarf.hero

import com.destroystokyo.paper.ParticleBuilder
import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.core.objects.DarknessManager
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.*
import com.jackob.dvz.util.CombinationUtil.ClickType.LEFT
import com.jackob.dvz.util.CombinationUtil.ClickType.RIGHT
import com.jackob.dvz.util.CombinationUtil.Sequence
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.entity.Display.Brightness
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Wizard(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher>, Listener {

    override val disguiseTemplate: Disguise = createPlayerDisguise("wizard", "Wizard") {}

    private val manaBank = ManaUtil(ownerId, 35, WIZARD_STAFF_KEY)

    private val staffCombinations = CombinationUtil(owner, WIZARD_STAFF_KEY, ::putOnCooldown).apply {
        registerAction(Sequence(RIGHT, LEFT, LEFT), ::launchBoulder)
        registerAction(Sequence(RIGHT, RIGHT, LEFT), ::spawnIceShards)
        registerAction(Sequence(RIGHT, RIGHT, RIGHT), ::teleport)
        registerAction(Sequence(RIGHT, LEFT, RIGHT), ::heal)
    }

    private var healTask: BukkitTask? = null

    private var lastBaseSpellCast: Long = 0

    companion object {
        private val WIZARD_STAFF_KEY = NamespacedKey(DvZ.INSTANCE, "dvz-wizard-staff")

        private const val BOULDER_SPELL_COST = 100
        private const val ICE_SHARDS_SPELL_COST = 100
        private const val TELEPORT_SPELL_COST = 100
        private const val HEAL_SPELL_COST = 100

        private const val BASE_SPELL_COOLDOWN = 1400L

        private const val BOULDER_WAVE_RADIUS = 15
        private const val ICE_SHARDS = 16

        private val wizardStaff = createItem(Material.IRON_HOE) {
            name = "<b><aqua>Wizard Staff"
            description = """
               ?  
            """

            persistentDataContainer.set(WIZARD_STAFF_KEY, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(ManaUtil.MANA_ITEM, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(DarknessManager.RADIANCE, PersistentDataType.BOOLEAN, true)
        }

        private val boulderHitParticles = Particle.WAX_OFF.builder().count(1)
        private val iceShardHitParticles = Particle.BLOCK.builder()
            .data(Material.LARGE_AMETHYST_BUD.createBlockData())
            .offset(0.5, 0.5, 0.5)
            .count(10)
            .extra(0.0)
    }

    override fun onActivate() {
        super.onActivate()

        val player = ownerId.toPlayer()!!
        startDisguise(player)
        player.inventory.addItem(wizardStaff)
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    override fun onDeactivate() {
        super.onDeactivate()

        stopDisguise(ownerId.toPlayer()!!)
        manaBank.unregisterManaBank()
        staffCombinations.unregisterCombinations()

        healTask?.cancel()
        HandlerList.unregisterAll(this)
    }

    private fun putOnCooldown(player: Player, time: Long = System.currentTimeMillis()) {
        lastBaseSpellCast = time
        player.setCooldown(wizardStaff.type, (BASE_SPELL_COOLDOWN / 1000.0 * 20).toInt())
    }

    private fun spawnBoulderFragment(location: Location, world: World, minSize: Double, maxSize: Double): BlockDisplay {
        require(minSize < maxSize)
        val matrix = Matrix4f()
            .scale(Random.nextDouble(minSize, maxSize).toFloat())
            .rotateXYZ(
                Math.toRadians(360 - Random.nextDouble(10.0, 45.0)).toFloat(),
                0f,
                Math.toRadians(Random.nextDouble(10.0, 45.0)).toFloat(),
            )

        return world.spawn(location, BlockDisplay::class.java) {
            it.block = Material.STONE.createBlockData()
            it.setTransformationMatrix(matrix)
            it.teleportDuration = 2
        }
    }

    private fun spawnBoulder(location: Location): Collection<Display> {
        val world = location.world
        val locations = location.getSphere(3, true)
        val fragments = ArrayList<Display>(locations.size)

        for (l in locations) {
            fragments.add(spawnBoulderFragment(l, world, 0.5, 2.0))
        }

        return fragments
    }

    private fun spawnExplosionFragment(location: Location, world: World, visualPart: Entity): ArmorStand {
        return world.spawn(location, ArmorStand::class.java) {
            it.isInvulnerable = true
            it.isCollidable = false
            it.isInvisible = true
            it.isSmall = true
            it.addPassenger(visualPart)
        }
    }

    private fun damageEnemiesOnHit(location: Location, wizardPlayer: Player) {
        val potionEffect = PotionEffect(PotionEffectType.NAUSEA, 6 * 20, 2, false, false)
        val locVector = location.toVector()
        for (e in location.getNearbyLivingEntities(BOULDER_WAVE_RADIUS.toDouble())) {
            if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
            if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

            e.damage(10.0, wizardPlayer)
            e.addPotionEffect(potionEffect)
            val pushVector = e.location.toVector().subtract(locVector).normalize().multiply(1.1)
            pushVector.y = 1.1
            e.velocity = pushVector
        }
    }

    private fun playHitGroundEffect(location: Location, player: Player) {
        val world = location.world
        val fragmentsCount = Random.nextInt(8, 20)
        val explosionFragments = ArrayList<Entity>(fragmentsCount)

        for (i in 0 until fragmentsCount) {
            val boulderFragment = spawnBoulderFragment(location, world, 0.1, 1.5)
            val stand = spawnExplosionFragment(location, world, boulderFragment)

            explosionFragments.add(stand)
            explosionFragments.add(boulderFragment)

            stand.velocity = Vector(
                Random.nextDouble(-3.0, 3.0).toFloat(),
                Random.nextDouble(1.1, 3.0).toFloat(),
                Random.nextDouble(-3.0, 3.0).toFloat()
            )
        }

        sync(delay = TimeUnit.SECONDS(4)) {
            explosionFragments.forEach(Entity::remove)
        }

        location.add(Vector(0.0, 0.3, 0.0))
            .playWaveEffect(BOULDER_WAVE_RADIUS, boulderHitParticles, WaveDirection.OUT, TimeUnit.TICKS(2))
        player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
    }

    private fun launchBoulder(player: Player) {
        val result = player.rayTraceBlocks(35.0)
        val tracedBlock = result?.hitBlock

        if (tracedBlock == null) {
            player.sendActionBar("<yellow>You cannot launch it there!".mm())
            return
        }

        player.withMana(manaBank, BOULDER_SPELL_COST) {
            val fallingFactor = 0.25f
            val spawnVector = eyeLocation.direction.normalize().multiply(5)
            val location = eyeLocation.add(spawnVector).add(Vector(0.0, 10.0, 0.0))
            val dirVector =
                tracedBlock.location.toVector().subtract(location.toVector()).normalize().multiply(fallingFactor)

            val fragments = spawnBoulder(location)

            var hitGround = false
            var counter = TimeUnit.SECONDS(3)
            sync(delay = TimeUnit.TICKS(1), period = TimeUnit.TICKS(1)) {
                if (!location.block.isPassable && !hitGround) {
                    hitGround = true
                    playHitGroundEffect(location.clone(), this@withMana)
                    damageEnemiesOnHit(location.clone(), player)
                }

                if (hitGround) {
                    counter--
                }

                if (counter <= 0) {
                    cancel()
                    fragments.forEach { it.remove() }
                    return@sync
                }

                fragments.forEach {
                    val location = it.location
                    location.add(dirVector)
                    it.teleport(location)
                }

                world.spawnParticle(Particle.ASH, location.clone().add(0.0, 2.0, 0.0), 8, 1.0, 1.0, 1.0)
                location.add(dirVector)
            }
            playSound(this.location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
        }

    }

    /**
     * Applies callback fn to Dwarf enemies in provided radius
     */
    private fun Location.areaEffect(radius: Double = 1.5, applyEffect: (LivingEntity) -> Unit) {
        for (e in getNearbyLivingEntities(radius)) {
            if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
            if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

            applyEffect(e)
        }
    }

    private fun iceShardHitEffect(location: Location, wizardPlayer: Player) {
        location.areaEffect {
            it.damage(2.0, wizardPlayer)
            it.freezeTicks = 25 * 20
        }

        iceShardHitParticles.location(location).receivers(30, true).spawn()
    }

    private fun spawnShard(location: Location): Pair<BlockDisplay, Matrix4f> {
        val randomAngle = Math.toRadians(Random.nextDouble(-90.0, 90.0)).toFloat()
        val matrix = Matrix4f().rotateX(randomAngle)

        val shard = location.world.spawn(location, BlockDisplay::class.java) {
            it.block = Material.LARGE_AMETHYST_BUD.createBlockData()
            it.interpolationDelay = 2
            it.interpolationDuration = 5
            it.setTransformationMatrix(matrix)
            it.brightness = Brightness(15, 15)
        }

        sync(delay = TimeUnit.TICKS(2)) {
            matrix.scale(0.8f, 10f, 0.8f)
            shard.setTransformationMatrix(matrix)
        }

        return Pair(shard, matrix)
    }

    private fun spawnIceShards(player: Player) = player.withMana(manaBank, ICE_SHARDS_SPELL_COST) {
        val vector = eyeLocation.direction.normalize()
        vector.y = 0.0
        val location = location.add(vector.clone().multiply(2)).subtract(Vector(0.0, 1.0, 0.0))
        location.yaw = 0f
        location.pitch = 0f

        val shards: Deque<Pair<BlockDisplay, Matrix4f>> = LinkedList()

        var counter = ICE_SHARDS
        sync(period = TimeUnit.TICKS(2)) {
            shards.addLast(spawnShard(location))
            iceShardHitEffect(location.clone().add(Vector(0.0, 2.0, 0.0)), this@withMana)
            location.add(vector)
            world.playSound(location, Sound.BLOCK_GLASS_BREAK, 3f, 1f)

            counter--
            if (counter <= 0) {
                cancel()
                removeIceShards(shards)
            }
        }

    }

    private fun removeIceShards(spikes: Deque<Pair<BlockDisplay, Matrix4f>>) {
        var counter = ICE_SHARDS
        sync(period = TimeUnit.TICKS(10)) {
            val shard = spikes.pollFirst()
            val matrix = shard.second.scale(0.8f, 0.1f, 0.8f)
            shard.first.interpolationDuration = 20
            shard.first.interpolationDelay = 2
            shard.first.setTransformationMatrix(matrix)
            spikes.addLast(shard)

            counter--
            if (counter <= 0) {
                cancel()
                sync(delay = TimeUnit.SECONDS(2)) {
                    spikes.forEach { it.first.remove() }
                    spikes.clear()
                }
            }
        }
    }

    private fun teleport(player: Player) = player.withMana(manaBank, TELEPORT_SPELL_COST) {
        val teleportRange = 15
        val direction = eyeLocation.direction.normalize()
        val destination = location

        val particles = Particle.DRIPPING_LAVA.builder().count(5).offset(0.5, 0.2, 0.5)

        var idx = 0
        while (idx < teleportRange) {
            destination.add(direction)
            particles.location(destination).spawn()
            destination.areaEffect { it.fireTicks = 8 * 20 }

            if (!destination.block.isPassable) {
                destination.subtract(direction)
                break
            }

            idx++
        }

        sync(delay = TimeUnit.TICKS(1)) {
            teleport(destination)
        }
    }

    private fun playPartialCircleEffect(
        angles: Array<IntRange>,
        radius: Int,
        loc: Location,
        particles: ParticleBuilder
    ) {
        for (angle in angles) {
            for (i in angle) {
                val radians = Math.toRadians(i.toDouble())
                val x = cos(radians) * radius
                val z = sin(radians) * radius

                loc.add(x, 0.0, z)
                particles.location(loc).spawn()
                loc.subtract(x, 0.0, z)
            }
        }
    }

    private fun playHealVisualEffect(location: Location) {
        val loc = location.add(Vector(0.0, 0.2, 0.0))
        val outwardParticles = Particle.BLOCK_CRUMBLE.builder().data(Material.PALE_OAK_WOOD.createBlockData())
        val inwardParticles = Particle.DUST.builder().color(Color.LIME).count(1)
        var radius = 1

        val outwardAngles = arrayOf(30..60, 120..150, 210..240, 300..330)
        val inwardAngles = arrayOf(61..119, 151..209, 241..299, 331..359, 0..29)

        sync(period = TimeUnit.TICKS(2)) {
            playPartialCircleEffect(outwardAngles, radius, loc, outwardParticles)

            radius++
            if (radius >= 5) {
                cancel()

                sync(delay = TimeUnit.TICKS(3), period = TimeUnit.TICKS(3)) {
                    playPartialCircleEffect(inwardAngles, radius, loc, inwardParticles)

                    radius--
                    if (radius <= 0) {
                        cancel()
                    }
                }
            }
        }
    }

    private fun heal(player: Player) = player.withMana(manaBank, HEAL_SPELL_COST) {
        healTask?.cancel()

        var healPotionEffect = PotionEffect(PotionEffectType.REGENERATION, 2 * 20, 4, false, false)
        var repetitions = 3
        healTask = sync(period = TimeUnit.SECONDS(3)) {
            addPotionEffect(healPotionEffect)
            for (e in location.getNearbyLivingEntities(5.0)) {
                if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) {
                    e.addPotionEffect(healPotionEffect)
                }
            }
            healPotionEffect = healPotionEffect.withAmplifier(healPotionEffect.amplifier - 1)
                .withDuration(healPotionEffect.duration + 20)

            playHealVisualEffect(location)

            repetitions--
            if (repetitions <= 0) {
                cancel()
                healTask = null
            }
        }

    }

    private fun castBaseSpell(player: Player) {
        val now = System.currentTimeMillis()
        if (now - lastBaseSpellCast < BASE_SPELL_COOLDOWN) return

        val red = Random.nextInt(256)
        val green = Random.nextInt(256)
        val blue = Random.nextInt(256)
        val spellParticle = Particle.Spell(Color.fromRGB(red, green, blue), 1f)

        val particles = Particle.INSTANT_EFFECT.builder().data(spellParticle).count(6)
        val dir = player.eyeLocation.direction.normalize()
        val start = player.eyeLocation
        val range = 12
        val step = 0.5

        var i = 0.0
        sync(period = TimeUnit.TICKS(2)) {
            if (i > range) {
                cancel()
                return@sync
            }

            i += step
            dir.multiply(i)
            start.add(dir)
            particles.location(start).receivers(range * 2, true).spawn()
            start.areaEffect { it.damage(5.0, player) }
            start.subtract(dir)
            dir.normalize()
        }

        player.playSound(player.location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f)
        putOnCooldown(player, now)
    }

    @EventHandler
    fun onStaffClick(e: PlayerInteractEvent) {
        if (staffCombinations.hasActiveCombination()) return
        val player = e.player
        if (player.uniqueId != ownerId) return

        val clickedItem = e.leftClickItem ?: return
        if (!clickedItem.persistentDataContainer.has(WIZARD_STAFF_KEY)) return

        castBaseSpell(player)
    }

}
