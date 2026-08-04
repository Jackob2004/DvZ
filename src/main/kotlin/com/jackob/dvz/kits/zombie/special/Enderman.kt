package com.jackob.dvz.kits.zombie.special

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.events.PortalCreateEvent
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler.Companion.UNPLACEABLE_KEY
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.*
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.EndermanWatcher
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.Vibration.Destination.BlockDestination
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPortalEnterEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.joml.Matrix4f
import java.util.*
import kotlin.random.Random


class Enderman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<EndermanWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.ENDERMAN) {
        isAggressive = false
    }

    override val aiZombieEnabled: Boolean = false

    companion object {
        private const val TELEPORT_COOLDOWN = 3

        private const val SCREAM_COOLDOWN = 3

        private const val SHARD_COOLDOWN = 3

        private val teleportCooldowns = CooldownUtil(TELEPORT_COOLDOWN * 1000L)

        private val screamCooldowns = CooldownUtil(SCREAM_COOLDOWN * 1000L)

        private val shardCooldowns = CooldownUtil(SHARD_COOLDOWN * 1000L)

        private val chainItem = createItem(Material.WAXED_EXPOSED_COPPER_CHAIN) {
            name = "<b><gray>Rusty Chain"
            description = """
                ?
            """
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }

        private val portalItem = createItem(Material.END_STONE) {
            name = "<b><light_purple>Portal"
            description = """
                ?
            """
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }
    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        startDisguise(player)

        player.inventory.addItem(chainItem, portalItem)
        player.playSound(player.location, Sound.ENTITY_ENDERMAN_AMBIENT, 1f, 1f)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    init {
        EndermanListener
    }

    private fun blinkEffect(location: Location) {
        val effectRange = 8.0
        val darknessEffect = PotionEffect(PotionEffectType.DARKNESS, 5 * 20, 0, false, false)

        for (p in location.getNearbyPlayers(effectRange)) {
            if (GameManager.getPlayerTeam(p) != TeamType.DWARF) continue

            p.addPotionEffect(darknessEffect)
        }

        Particle.WITCH.builder()
            .location(location)
            .offset(1.5, 1.5, 1.5)
            .count(10)
            .extra(0.0)
            .receivers(12, true)
            .spawn()
    }

    private fun teleport(endermanPlayer: Player) {
        if (teleportCooldowns.isOnCooldownSafe(endermanPlayer)) {
            teleportCooldowns.displayCooldown(endermanPlayer)
            return
        }

        val teleportRange = 35.0
        val oldLoc = endermanPlayer.eyeLocation

        val result = endermanPlayer.rayTraceBlocks(teleportRange)
        val targetBlock = result?.hitBlock

        if (targetBlock == null) {
            endermanPlayer.sendActionBar("<yellow>Cannot teleport there".mm())
            return
        }

        val destination = targetBlock.location.add(0.0, 1.0, 0.0)
        destination.yaw = oldLoc.yaw
        destination.pitch = oldLoc.pitch

        endermanPlayer.teleport(destination)
        blinkEffect(oldLoc)
        teleportCooldowns.putOnCooldown(endermanPlayer)

        endermanPlayer.playSound(endermanPlayer.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
    }

    private fun getEnemiesInLine(endermanPlayer: Player): Set<Player> {
        val enemies = mutableSetOf<Player>()

        val range = 9
        val step = 3.0
        val distanceVector = endermanPlayer.eyeLocation.direction.normalize().multiply(step)

        var rangeCovered = 0
        val currPoint = endermanPlayer.eyeLocation

        while (rangeCovered < range) {
            currPoint.add(distanceVector)
            rangeCovered += step.toInt()

            for (e in currPoint.getNearbyEntities(step, step, step)) {
                val dwarfEnemy = e as? Player ?: continue
                if (GameManager.getPlayerTeam(dwarfEnemy) != TeamType.DWARF) continue
                if (dwarfEnemy.uniqueId == ownerId) continue

                enemies.add(dwarfEnemy)
            }
        }

        return enemies
    }

    private fun scream(endermanPlayer: Player) = endermanPlayer.withCooldown(screamCooldowns) {
        modifyMobDisguise { isAggressive = true }
        var enemies = getEnemiesInLine(endermanPlayer)
        val repetitions = 6
        val period = 15

        val slowness = PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 4, false, false)
        val darkness = PotionEffect(PotionEffectType.DARKNESS, 5 * 20, 0, false, false)

        var counter = repetitions
        sync(period = TimeUnit.TICKS(period.toLong())) {
            if (!this@withCooldown.isOnline) {
                cancel()
                return@sync
            }

            for (e in enemies) {
                e.addPotionEffect(slowness)
                e.addPotionEffect(darkness)
                e.damage(4.0, this@withCooldown)

                Particle.VIBRATION.builder()
                    .location(eyeLocation)
                    .data(Vibration(BlockDestination(e.location), period))
                    .receivers(32, true)
                    .spawn()
            }
            playSound(location, Sound.ENTITY_ENDERMAN_SCREAM, 1f, 1f)

            counter--
            if (counter <= 0) {
                cancel()
                this@withCooldown.modifyMobDisguise { isAggressive = false }
            }
            enemies = getEnemiesInLine(endermanPlayer)
        }

        addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, (repetitions * period), 4, false, false))
    }

    private fun launchChain(endermanPlayer: Player) = endermanPlayer.withCooldown(shardCooldowns) {
        val chain = ChainWound.spawnChain(eyeLocation) {
            translate(-0.5f, 0f, 0f)
        }

        val stand = world.spawn(eyeLocation, ArmorStand::class.java) { s ->
            s.isInvisible = true
            s.addPassenger(chain)
            s.velocity = this.eyeLocation.direction.normalize().multiply(3.0)
        }

        val particles = Particle.REVERSE_PORTAL.builder().extra(0.0).count(5).offset(0.0, 0.0, 0.0)

        val htiAreaRadius = 1.0

        sync(period = TimeUnit.TICKS(1), delay = TimeUnit.TICKS(1)) {
            if (stand.isOnGround) {
                cancel()
                chain.remove()
                stand.remove()
                return@sync
            }
            val location = stand.location
            val entity = location.getNearbyPlayers(htiAreaRadius)
                .firstOrNull { ChainWound.canInflict(it) }

            if (entity != null) {
                entity.damage(6.0, this@withCooldown)
                cancel()
                chain.remove()
                stand.remove()

                ChainWound(ownerId, entity)
            }

            particles.location(location).receivers(20, true).spawn()
        }

        playSound(location, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f)
    }

    private fun spawnPortal(endermanPlayer: Player) {
        val heightOffset = 2.0
        val vector = endermanPlayer.eyeLocation.direction.normalize().multiply(4.0)
        val location = endermanPlayer.location.add(0.0,heightOffset, 0.0).add(vector)
        val portalEvent = PortalCreateEvent(location)
        portalEvent.callEvent()

        if (portalEvent.isCancelled) {
            endermanPlayer.sendActionBar("<yellow>Cannot create portal here!".mm())
            return
        }

        endermanPlayer.sendActionBar("<green>created!".mm())

        if (!Portal.canCreate(location, endermanPlayer)) {
            endermanPlayer.sendActionBar("<yellow>Cannot create portal here!".mm())
            return
        }

        Portal(location, heightOffset, endermanPlayer)
        endermanPlayer.removeItem(portalItem, 1)
    }

    object EndermanListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onItemClick(e: PlayerInteractEvent) {
            val player = e.player
            val endermanKit = KitsManager.getKit(player) as? Enderman ?: return
            val rightClicked = e.rightClickItem ?: return

            when (rightClicked.type) {
                Material.COMPASS -> endermanKit.teleport(player)
                Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE -> endermanKit.scream(player)
                chainItem.type -> endermanKit.launchChain(player)
                portalItem.type -> endermanKit.spawnPortal(player)
                else -> Unit
            }

        }
    }

    private class Portal(val location: Location, val heightOffset: Double, val endermanPlayer: Player): Listener {

        private val portalBlockLocations: ArrayList<Location> = ArrayList()

        private val fragments: ArrayList<BlockDisplay> = ArrayList(FRAGMENTS_COUNT)

        private var hitbox: Interaction? = null

        private var health: Int = MAX_HEALTH

        private var lastHit: Long = 0

        private var removalTask: BukkitTask? = null

        companion object {
            private const val FRAGMENTS_COUNT = 12
            private const val PORTAL_RADIUS = 3
            private const val HIT_INTERVAL = 800
            private const val MAX_HEALTH = 40

            fun canCreate(location: Location, player: Player) : Boolean {
                val sphere = location.getSphere(PORTAL_RADIUS, true)

                return !sphere.any { !it.block.isBreakable(player) || it.block.type != Material.AIR }
            }
        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
            createPortal(location)
        }

        private fun createHitbox(location: Location) {
            hitbox = location.world.spawn(location, Interaction::class.java) {
                val dimensions = 5f
                it.interactionWidth = dimensions
                it.interactionHeight= dimensions
                it.isResponsive = true
            }
        }

        private fun startRemovalTask() {
            removalTask = sync(delay = TimeUnit.SECONDS(MAX_HEALTH.toLong())) {
                removePortal()
            }
        }

        private fun createPortal(location: Location) {
            val world = location.world
            location.getSphere(PORTAL_RADIUS, true).forEach { portalBlockLocations.add(it) }
            for (l in portalBlockLocations) {
                l.block.type = Material.END_PORTAL
            }

            val matrix = Matrix4f().scale(0.4f)

            for (i in 1..FRAGMENTS_COUNT) {
                val offsetX = Random.nextDouble(-3.0, 3.0)
                val offsetY = Random.nextDouble(-3.0, 3.0)
                val offsetZ = Random.nextDouble(-3.0, 3.0)
                val rndAngle = Random.nextDouble(50.0, 180.0)
                val loc = location.clone().add(offsetX, offsetY, offsetZ)

                val fragmentDisplay = world.spawn(loc, BlockDisplay::class.java) {
                    it.block = Material.CRYING_OBSIDIAN.createBlockData()
                    it.brightness = Display.Brightness(15, 15)
                    it.interpolationDelay = 20
                    it.interpolationDuration = 60
                }
                sync(delay = TimeUnit.SECONDS(1)) {
                    val angle = Math.toRadians(rndAngle).toFloat()
                    fragmentDisplay.setTransformationMatrix(matrix.rotateX(angle).rotateY(angle).rotateZ(angle))
                }

                fragments.add(fragmentDisplay)
            }

            createHitbox(location.clone().subtract(0.0,heightOffset,0.0))
            startRemovalTask()
            world.playSound(location, Sound.BLOCK_END_PORTAL_SPAWN, 1f, 1f)
        }

        private fun removePortal() {
            for (f in fragments) {
                f.remove()
            }

            for (b in portalBlockLocations) {
                b.block.type = Material.AIR
            }
            hitbox!!.remove()
            hitbox = null

            if (!removalTask!!.isCancelled) {
                removalTask!!.cancel()
            }
            removalTask = null

            HandlerList.unregisterAll(this)
            location.subtract(0.0,heightOffset,0.0).createExplosion(endermanPlayer,3f, false, false)
        }

        private fun takeDamage() {
            val now = System.currentTimeMillis()
            if (now - lastHit < HIT_INTERVAL) return
            lastHit = now

            health--

            if (health <= 0) {
                removePortal()
            }

            portalBlockLocations.removeLastOrNull()?.block?.type = Material.AIR
            portalBlockLocations.removeLastOrNull()?.block?.type = Material.AIR
        }

        @EventHandler
        fun onScrollClick(e: PlayerInteractEvent) {
            val rightClicked = e.rightClickItem ?: return
            if (rightClicked.type != Material.FLOWER_BANNER_PATTERN) return
            val player = e.player
            if (GameManager.getPlayerTeam(player) != TeamType.ZOMBIE) return

            player.removeItem(rightClicked, 1)
            player.teleport(location)
        }

        @EventHandler
        fun onHitboxHit(e: EntityDamageByEntityEvent) {
            if (hitbox == null) return

            if (e.entity != hitbox) return
            val damager = e.damager as? Player ?: return
            if (GameManager.getPlayerTeam(damager) != TeamType.DWARF) return

            takeDamage()
        }

        @EventHandler
        fun onPortalEnter(e: EntityPortalEnterEvent) {
            e.isCancelled = true
        }
    }

    private class ChainWound(val endermanPlayerId: UUID, victim: Player) : Listener {

        private val victimId = victim.uniqueId

        private var display: BlockDisplay? = null

        private var healingProcess: BukkitTask? = null

        private var bleedingTask: BukkitTask? = null

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
            createChain(victim)
        }

        companion object {
            fun canInflict(player: Player): Boolean {
                return GameManager.getPlayerTeam(player) != TeamType.ZOMBIE && player.passengers.isEmpty()
            }

            fun spawnChain(location: Location, modifyMatrix: Matrix4f.() -> Unit): BlockDisplay {
                val matrix = Matrix4f()
                    .scale(2.5f, 2.5f, 5f)
                    .rotateX(Math.toRadians(90.0).toFloat())
                matrix.modifyMatrix()

                return location.world.spawn(location, BlockDisplay::class.java) { display ->
                    display.brightness = Display.Brightness(15, 15)
                    display.block = chainItem.type.createBlockData()
                    display.setTransformationMatrix(matrix)
                }
            }

            private val bloodParticles = Particle.BLOCK.builder()
                .data(Material.REDSTONE_BLOCK.createBlockData())
                .offset(0.5, 0.5, 0.5)
                .count(10)
                .extra(0.0)

            private val slowness = PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 1, false, false)

            private val nausea = PotionEffect(PotionEffectType.NAUSEA, 7 * 20, 1, false, false)
        }

        private fun createChain(victim: Player) {
            display = spawnChain(victim.location) {
                translate(-0.5f, -0.5f, -0.25f)
            }

            victim.addPassenger(display!!)
            victim.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 1000000, 1, false, false))
            startBleedingTask()
        }

        private fun startBleedingTask() {
            val delay = 7L + Random.nextInt(10)
            bleedingTask = sync(delay = TimeUnit.SECONDS(delay)) {
                val victim = victimId.toPlayer()
                if (victim == null) {
                    cancel()
                    return@sync
                }

                woundBleedEffect(victim, 2.0, nausea)
                startBleedingTask()
            }
        }

        private fun removeWound() {
            HandlerList.unregisterAll(this)
            display!!.remove()
            display = null

            if (healingProcess != null) {
                healingProcess!!.cancel()
                healingProcess = null
            }

            if (bleedingTask != null) {
                bleedingTask!!.cancel()
                bleedingTask = null
            }
        }

        private fun woundBleedEffect(victim: Player, damage: Double, potionEffect: PotionEffect) {
            val location = victim.location
            val damager = endermanPlayerId.toPlayer()?.takeIf { KitsManager.getKit(it) == Enderman }

            victim.damage(damage, damager)
            victim.playSound(location, Sound.ENTITY_ENDERMAN_HURT, 1f, 1f)
            victim.showTitle(Title.title("<dark_red>Wound bleeding".mm(), "".mm()))
            victim.addPotionEffect(potionEffect)

            bloodParticles
                .location(location.add(0.0, 1.2, 0.0))
                .receivers(15, true)
                .spawn()
        }

        private fun startHealing(victim: Player) {
            if (healingProcess != null) return
            val timeToHeal = 10

            var counter = 0
            healingProcess = sync(period = TimeUnit.SECONDS(1)) {
                if (!victim.isOnline) {
                    cancel()
                    return@sync
                }

                counter++
                victim.showTitle(Title.title("<aqua>$counter/$timeToHeal".mm(), "<green>Healing".mm()))

                if (counter >= timeToHeal) {
                    cancel()
                    removeWound()
                    victim.removePotionEffect(PotionEffectType.WEAKNESS)
                    sync(delay = TimeUnit.SECONDS(1)) {
                        victim.showTitle(Title.title("<green>Fully healed".mm(), "".mm()))
                    }
                }
            }
        }

        private fun stopHealing(victim: Player) {
            if (healingProcess == null) return
            healingProcess!!.cancel()
            healingProcess = null

            victim.showTitle(Title.title("<yellow>Healing interrupted".mm(), "".mm()))
        }

        @EventHandler
        fun onVictimSneak(e: PlayerToggleSneakEvent) {
            val player = e.player
            if (player.uniqueId != victimId) return

            if (e.isSneaking) {
                startHealing(player)
            } else {
                stopHealing(player)
            }

        }

        @EventHandler
        fun onVictimJump(e: PlayerJumpEvent) {
            if (e.player.uniqueId == victimId) {
                woundBleedEffect(e.player, 4.0, slowness)
            }
        }

        @EventHandler
        fun onVictimDeath(e: PlayerDeathEvent) {
            if (e.player.uniqueId == victimId) {
                removeWound()
            }
        }

        @EventHandler
        fun onVictimQuit(e: PlayerQuitEvent) {
            if (e.player.uniqueId != victimId) return

            if (display != null) {
                display!!.remove()
                display = null
            }

            if (bleedingTask != null) {
                bleedingTask!!.cancel()
                bleedingTask = null
            }
        }

        @EventHandler
        fun onVictimRejoin(e: PlayerJoinEvent) {
            if (e.player.uniqueId == victimId) {
                createChain(e.player)
            }
        }

    }
}