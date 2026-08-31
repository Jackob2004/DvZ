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
import com.jackob.dvz.util.CombinationUtil.ClickType.*
import com.jackob.dvz.util.CombinationUtil.Sequence
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.util.*
import kotlin.math.sqrt

class Shaman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher>, Listener {

    override val disguiseTemplate: Disguise = createPlayerDisguise("shaman", "Shaman") {}

    private val manaBank = ManaUtil(owner, 40, SHAMAN_STAFF)

    private val staffCombinations = CombinationUtil(owner, SHAMAN_STAFF, ::putOnCooldown).apply {
        registerAction(Sequence(RIGHT, LEFT, RIGHT), ::spawnTotem)
        registerAction(Sequence(RIGHT, RIGHT, RIGHT), ::leapTowardsTotem)
        registerAction(Sequence(RIGHT, LEFT, LEFT), ::pushEnemies)
        registerAction(Sequence(RIGHT, RIGHT, LEFT), ::pullEnemies)
    }

    private var totem: ArmorStand? = null

    private var totemModel: ItemDisplay? = null

    private var totemInfoBar: TextDisplay? = null

    private var totemTickingTask: BukkitTask? = null

    private var lastBaseSpellCast: Long = 0

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    companion object {
        private const val TOTEM_RANGE = 10.0
        private const val BASE_SPELL_COOLDOWN = 500L

        private const val SPAWN_TOTEM_COST = 300
        private const val LEAP_COST = 200
        private const val PUSH_ENEMIES_COST = 350
        private const val PULL_ENEMIES_COST = 250

        private val SHAMAN_STAFF = NamespacedKey(DvZ.INSTANCE, "shaman-staff-item")

        private val shamanStaff = createItem(Material.WOODEN_HOE) {
            name = "<b><dark_green>Shaman Staff"
            description = """
                A powerful item capable of casting spells.

                Left-click to cast your primary attack.
                Right-click to initiate spell combinations, draining from a <light_purple>[1000]<reset> mana bank.

                <gray><u>Combinations:
                <green>[RLR] <gray>-<white> Summon a totem <light_purple>[$SPAWN_TOTEM_COST]
                <green>[RRR] <gray>-<white> Leap toward the totem <light_purple>[$LEAP_COST]
                <green>[RLL] <gray>-<white> Push enemies away from the totem <light_purple>[$PUSH_ENEMIES_COST]
                <green>[RRL] <gray>-<white> Pull enemies toward the totem <light_purple>[$PULL_ENEMIES_COST]
            """

            persistentDataContainer.set(ManaUtil.MANA_ITEM, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(SHAMAN_STAFF, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(DarknessManager.RADIANCE, PersistentDataType.BOOLEAN, true)
        }

        private val totemPositiveEffect = PotionEffect(PotionEffectType.REGENERATION, 2 * 20, 0, false, false)
        private val totemPullEffect = PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 1, false, false)

        private val defaultParticles = Particle.ENCHANT.builder().count(1).extra(0.0)
        private val pushParticles = Particle.BUBBLE_POP.builder().count(1).extra(0.0)
        private val pullParticles = Particle.SQUID_INK.builder().count(1).extra(0.0)
        val baseSpellParticles = Particle.ENCHANTED_HIT.builder().count(1).extra(0.0)
    }

    override fun onActivate() {
        super.onActivate()

        val player = ownerId.toPlayer()!!
        startDisguise(player)
        player.inventory.addItem(shamanStaff)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!

        stopDisguise(player)
        removeTotem()
        manaBank.unregisterManaBank()
        staffCombinations.unregisterCombinations()
        HandlerList.unregisterAll(this)
    }

    private fun putOnCooldown(player: Player, time: Long = System.currentTimeMillis()) {
        lastBaseSpellCast = time
        player.setCooldown(shamanStaff.type, (BASE_SPELL_COOLDOWN / 1000.0 * 20).toInt())
    }

    private fun removeTotem() {
        if (totem != null) {
            totem!!.remove()
            totem = null
        }

        if (totemModel != null) {
            totemModel!!.remove()
            totemModel = null
        }

        if (totemInfoBar != null) {
            totemInfoBar!!.remove()
            totemInfoBar = null
        }

        if (totemTickingTask != null) {
            totemTickingTask!!.cancel()
            totemTickingTask = null
        }
    }

    private fun onTotemTick() {
        val location = totem!!.location
        for (entity in location.getNearbyLivingEntities(TOTEM_RANGE)) {
            if (entity is Player && GameManager.getPlayerTeam(entity) == TeamType.DWARF) {
                entity.addPotionEffect(totemPositiveEffect)
            } else if (entity.type == AIZombieScheduler.MOB_TYPE || (entity is Player && GameManager.getPlayerTeam(entity) == TeamType.ZOMBIE)) {
                entity.damage(2.0, ownerId.toPlayer())
            }
        }

        val effectRange = TOTEM_RANGE.toInt()
        location.add(0.0, 0.8, 0.0).playCircleEffect(defaultParticles, effectRange, effectRange + 5)
    }

    private fun spawnTotem(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, SPAWN_TOTEM_COST) {
        removeTotem()

        val vector = shamanPlayer.eyeLocation.direction.normalize().multiply(1.2)
        vector.y = 1.2

        val maxLifeTime = 30
        var countDown = maxLifeTime

        totemInfoBar = world.spawn(eyeLocation, TextDisplay::class.java) {
            val matrix = Matrix4f().translate(0f, 0.7f, 0f)
            it.billboard = Display.Billboard.CENTER
            it.text("<light_purple>⌛<gray> ${maxLifeTime}s".mm())
            it.setTransformationMatrix(matrix)
        }

        val totemModelLoc = eyeLocation
        totemModelLoc.pitch = 0f
        totemModelLoc.yaw = 0f
        totemModel = world.spawn(totemModelLoc, ItemDisplay::class.java) {
            val matrix = Matrix4f().scale(2f).translate(0f, -0.2f, 0f)
            it.setItemStack(ItemStack(Material.TOTEM_OF_UNDYING))
            it.setTransformationMatrix(matrix)
            it.addPassenger(totemInfoBar!!)
        }

        totem = world.spawn(location.add(0.0, 1.0, 0.0), ArmorStand::class.java) {
            it.isInvulnerable = true
            it.isInvisible = true
            it.velocity = vector
            it.addPassenger(totemModel!!)
        }

        totemTickingTask = sync(delay = TimeUnit.SECONDS(2), period = TimeUnit.SECONDS(1)) {
            onTotemTick()
            totemInfoBar!!.text("<light_purple>⌛<gray> ${countDown}s".mm())
            countDown--

            if (countDown <= 0) {
                removeTotem()
            }
        }

        playSound(location, Sound.ITEM_TOTEM_USE, 1f, 1f)
    }

    private fun leapTowardsTotem(shamanPlayer: Player) {
        if (totem == null) return

        val targetLocation = totem!!.eyeLocation
        val maxDistance = TOTEM_RANGE * 2
        var distance = targetLocation.distanceSquared(shamanPlayer.location)
        if (distance > maxDistance * maxDistance) return

        val yModifier = if (totem!!.isOnGround) 3.0 else -2.0
        targetLocation.add(0.0, yModifier, 0.0)
        distance = sqrt(distance)

        shamanPlayer.withMana(manaBank, LEAP_COST) {
            val percentage = distance * 100 / maxDistance / 100
            val pullForce = 3.5 * percentage
            val vector = targetLocation.toVector().subtract(location.toVector()).normalize().multiply(pullForce)
            velocity = vector

            playSound(location, Sound.ENTITY_GOAT_SCREAMING_LONG_JUMP, 1f, 1f)
        }
    }

    enum class WaveDirection(val number: Int) {
        IN(-1),
        OUT(1)
    }

    private fun playWaveEffect(location: Location, effect: ParticleBuilder, direction: WaveDirection) {
        val range = TOTEM_RANGE.toInt()
        var counter = if (direction == WaveDirection.IN) range else 1
        val limit = if (direction == WaveDirection.IN) 0 else range

        sync(period = TimeUnit.TICKS(3)) {
            location.playCircleEffect(effect, counter, range * 2)

            counter += direction.number
            if (counter == limit) {
                cancel()
            }
        }
    }

    private fun pushEnemies(shamanPlayer: Player) {
        if (totem == null) return

        shamanPlayer.withMana(manaBank, PUSH_ENEMIES_COST) {
            val totemLoc = totem!!.location.add(0.0, 0.8, 0.0)
            val totemVector = totemLoc.toVector()

            var counter = 3
            sync(period = TimeUnit.SECONDS(1)) {
                for (e in totemLoc.getNearbyLivingEntities(TOTEM_RANGE)) {
                    if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
                    if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

                    val vector = e.location.toVector().subtract(totemVector).normalize().multiply(3.0)
                    vector.y = 1.1
                    e.velocity = vector
                }
                playWaveEffect(totemLoc, pushParticles, WaveDirection.OUT)

                counter--
                if (counter <= 0) {
                    cancel()
                }
            }

            playSound(location, Sound.BLOCK_WATER_AMBIENT, 1f, 1f)
        }
    }

    private fun pullEnemies(shamanPlayer: Player) {
        if (totem == null) return

        shamanPlayer.withMana(manaBank, PULL_ENEMIES_COST) {
            val totemLoc = totem!!.location.add(0.0, 0.1, 0.0)
            val totemVector = totemLoc.toVector()
            var counter = 5
            sync(period = TimeUnit.SECONDS(1)) {
                for (e in totemLoc.getNearbyLivingEntities(TOTEM_RANGE)) {
                    if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
                    if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

                    val vector = totemVector.clone().subtract(e.location.toVector()).normalize().multiply(2.5)
                    vector.y = -1.1
                    e.velocity = vector
                    e.addPotionEffect(totemPullEffect)
                }
                playWaveEffect(totemLoc, pullParticles, WaveDirection.IN)

                counter--
                if (counter <= 0) {
                    cancel()
                }
            }

            playSound(location, Sound.ENTITY_SQUID_HURT, 1f, 1f)
        }
    }

    private fun dealDamage(location: Location, radius: Double, damageSource: Player) {
        for (e in location.getNearbyLivingEntities(radius)) {
            if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
            if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

            e.damage(3.0, damageSource)
        }
    }

    private fun drawLine(
        range: Int,
        step: Double,
        dir: Vector,
        start: Location,
        particles: ParticleBuilder,
        player: Player
    ) {
        var i = 0.0
        while (i < range) {
            i += step
            dir.multiply(i)
            start.add(dir)
            particles.location(start).receivers(10, true).spawn()
            dealDamage(start, step, player)
            start.subtract(dir)
            dir.normalize()
        }
    }

    private fun castBaseSpell(shamanPlayer: Player) {
        val now = System.currentTimeMillis()
        if (now - lastBaseSpellCast < BASE_SPELL_COOLDOWN) return

        val range = 10
        val step = 0.5

        val start = shamanPlayer.eyeLocation
        val dir = start.direction.normalize()
        val angle = 15.0
        val rightDir = dir.clone().rotateAroundY(Math.toRadians(angle)).normalize()
        val leftDir = dir.clone().rotateAroundY(Math.toRadians(-angle)).normalize()

        drawLine(range, step, dir, start, baseSpellParticles, shamanPlayer)
        drawLine(range, step, rightDir, start, baseSpellParticles, shamanPlayer)
        drawLine(range, step, leftDir, start, baseSpellParticles, shamanPlayer)

        shamanPlayer.playSound(shamanPlayer.location, Sound.ENCHANT_THORNS_HIT, 1f, 1f)
        putOnCooldown(shamanPlayer, now)
    }

    @EventHandler
    fun onStaffClick(e: PlayerInteractEvent) {
        if (staffCombinations.hasActiveCombination()) return
        val player = e.player
        if (player.uniqueId != ownerId) return

        val clickedItem = e.leftClickItem ?: return
        if (!clickedItem.persistentDataContainer.has(SHAMAN_STAFF)) return

        castBaseSpell(player)
    }

}