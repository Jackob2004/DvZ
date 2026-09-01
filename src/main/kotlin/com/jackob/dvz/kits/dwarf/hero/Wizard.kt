package com.jackob.dvz.kits.dwarf.hero

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
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.util.*
import kotlin.random.Random

class Wizard(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher>, Listener {

    override val disguiseTemplate: Disguise = createPlayerDisguise("wizard", "Wizard") {}

    private val manaBank = ManaUtil(ownerId, 35, WIZARD_STAFF_KEY)

    private val staffCombinations = CombinationUtil(owner, WIZARD_STAFF_KEY, ::onAnySpell).apply {
        registerAction(Sequence(RIGHT, LEFT, RIGHT), ::launchBoulder)
    }

    companion object {
        private val WIZARD_STAFF_KEY = NamespacedKey(DvZ.INSTANCE, "dvz-wizard-staff")

        private const val BOULDER_SPELL_COST = 100

        private const val BOULDER_WAVE_RADIUS = 15

        private val wizardStaff = createItem(Material.IRON_HOE) {
            name = "<b><aqua>Wizard Staff"
            description = """
               ?  
            """

            persistentDataContainer.set(WIZARD_STAFF_KEY, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(ManaUtil.MANA_ITEM, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(DarknessManager.RADIANCE, PersistentDataType.BOOLEAN, true)
        }

        private val hitParticles = Particle.WAX_OFF.builder().count(1)
    }

    override fun onActivate() {
        super.onActivate()

        val player = ownerId.toPlayer()!!
        startDisguise(player)
        player.inventory.addItem(wizardStaff)
    }

    override fun onDeactivate() {
        super.onDeactivate()

        stopDisguise(ownerId.toPlayer()!!)
        manaBank.unregisterManaBank()
        staffCombinations.unregisterCombinations()
    }

    private fun onAnySpell(player: Player) {

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
        val potionEffect = PotionEffect(PotionEffectType.NAUSEA, 6 * 20, 2,false, false)
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
            .playWaveEffect(BOULDER_WAVE_RADIUS, hitParticles, WaveDirection.OUT, TimeUnit.TICKS(2))
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

}
