package com.jackob.dvz.kits.dwarf.base

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler.Companion.UNPLACEABLE_KEY
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.getCube
import com.jackob.dvz.util.hasMoved
import com.jackob.dvz.util.leftClickItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.packCoordinates
import com.jackob.dvz.util.removeItem
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.updateItem
import com.jackob.dvz.util.withCooldown
import io.papermc.paper.entity.LookAnchor
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.kyori.adventure.title.Title
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.entity.Firework
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

class Builder(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero) {

    private val buildings: MutableList<Pair<BuildingType, Building>> = ArrayList(BUILDINGS_LIMIT)

    private var selectedBuildingIdx = 0

    private var selectedBuildingType = BuildingType.entries[selectedBuildingIdx]

    private var chosenBuilding: Building? = null

    private var buildingTask: BukkitTask? = null

    init {
        BuilderListener
    }

    companion object {
        private const val BUILDINGS_LIMIT = 10
        private const val HAMMER_PREFIX = "<aqua>Builder's Hammer - "

        private val buildingResource = Material.COBBLESTONE

        private val hammerItem = createItem(Material.IRON_INGOT) {
            name = HAMMER_PREFIX + BuildingType.entries[0].buildingName
            description = """
                ?    
            """
        }

    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        player.inventory.addItem(hammerItem)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        buildings.forEach { it.second.remove() }
        buildings.clear()
        chosenBuilding = null
    }

    private fun selectNextBuilding(builderPlayer: Player, item: ItemStack) {
        if (buildingTask != null) return

        selectedBuildingIdx = (selectedBuildingIdx + 1) % BuildingType.entries.size
        selectedBuildingType = BuildingType.entries[selectedBuildingIdx]

        item.updateItem { name = HAMMER_PREFIX + selectedBuildingType.buildingName }
        builderPlayer.playSound(builderPlayer.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)
    }

    private fun chooseBuilding() {
        chosenBuilding = when (selectedBuildingType) {
            BuildingType.BALLISTA -> Ballista()
            BuildingType.MORTAR -> Mortar()
            BuildingType.SUPPLY_BOX -> SupplyBox()
            BuildingType.MAGIC_DOORS -> MagicDoors()
        }
    }

    private fun calcResources(player: Player): Int = player.inventory.contents
        .filter { it != null && it.type == buildingResource}
        .sumOf { it!!.amount }

    private fun hasMaxBuildings(): Boolean {
        return buildings.count { it.first == selectedBuildingType } == selectedBuildingType.limit
    }

    private fun startBuildingProcess(builderPlayer: Player, location: Location) {
        var counter = 0
        val timeToBuild = selectedBuildingType.buildingTimeInSeconds
        buildingTask = sync(period = TimeUnit.SECONDS(1)) {
            if (builderPlayer.hasMoved(location)) {
                builderPlayer.showTitle(Title.title("<yellow>Building interrupted!".mm(), "".mm()))
                cancel()
                buildingTask = null
                return@sync
            }

            if (!builderPlayer.isOnline || builderPlayer.isDead) {
                cancel()
                buildingTask = null
                return@sync
            }

            builderPlayer.showTitle(Title.title("<aqua>$counter/$timeToBuild".mm(), "<gray>building...".mm()))
            builderPlayer.playSound(location, Sound.UI_STONECUTTER_TAKE_RESULT, 1f, 1f)

            counter++
            if (counter >= timeToBuild) {
                chosenBuilding!!.spawn(builderPlayer, location)
                buildings.add(Pair(selectedBuildingType, chosenBuilding!!))
                chosenBuilding = null
                builderPlayer.inventory.removeItem(ItemStack(Material.COBBLESTONE, selectedBuildingType.cost))

                sync(delay = TimeUnit.SECONDS(1)) {
                    builderPlayer.showTitle(Title.title("<green>Completed!".mm(), "".mm()))
                }

                cancel()
                buildingTask = null
            }
        }
    }

    private fun build(builderPlayer: Player) {
        if (buildingTask != null) return

        if (buildings.size == BUILDINGS_LIMIT) {
            builderPlayer.sendActionBar("<red>You reached limit of buildings".mm())
            builderPlayer.playSound(builderPlayer.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        if (hasMaxBuildings()) {
            builderPlayer.sendActionBar("<red>You cannot build more buildings of this type".mm())
            builderPlayer.playSound(builderPlayer.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val neededResources = selectedBuildingType.cost
        val playerResources = calcResources(builderPlayer)
        if (neededResources > playerResources) {
            val gap = neededResources - playerResources
            val name = selectedBuildingType.buildingName
            builderPlayer.sendActionBar("<white>You need <gray>$gap<reset> more to build $name".mm())
            builderPlayer.playSound(builderPlayer.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        chooseBuilding()
        val location = builderPlayer.location
        if (!chosenBuilding!!.canSpawn(location)) {
            builderPlayer.sendActionBar("<yellow>You cannot build it here!".mm())
            builderPlayer.playSound(builderPlayer.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        startBuildingProcess(builderPlayer, location)
    }

    enum class BuildingType(val buildingName: String, val cost: Int, val limit: Int, val buildingTimeInSeconds: Int){
        BALLISTA("<red>Ballista", 20, 2, 10),
        MORTAR("<dark_red>Mortar", 300, 1, 13),
        SUPPLY_BOX("<light_purple>Supply Box", 10, 3, 3),
        MAGIC_DOORS("<dark_aqua>Magic Doors", 5, 10, 3)
    }

    object BuilderListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onItemClick(event: PlayerInteractEvent) {
            val player = event.player
            val builderKit = KitsManager.getKit(player) as? Builder ?: return

            val rightClicked = event.rightClickItem
            val leftClicked = event.leftClickItem

            if (rightClicked?.type == hammerItem.type) {
                builderKit.build(player)
                event.isCancelled = true
            } else if (leftClicked?.type == hammerItem.type) {
                builderKit.selectNextBuilding(player, leftClicked)
                event.isCancelled = true
            }

        }
    }

    data class HitboxDimensions(val width: Float, val height: Float)

    data class BuildingConfig(
        val displayItem: ItemStack,
        val displayMatrix: Matrix4f,
        val displayLocModifier: Vector,
        val hitboxDimensions: HitboxDimensions,
        val hitboxLocModifier: Vector,
        val healthBarLocModifier: Vector,
        val maxBlocksAboveBedrock: Int,
        val name: String,
        val customProtectedArea: Boolean = false
    )

    private abstract class Building(val maxHealth: Int) : Listener {

        protected var display: ItemDisplay? = null

        protected var meleeHitbox: Interaction? = null

        private var arrowHitbox: ArmorStand? = null

        private var healthBar: TextDisplay? = null

        private var name: TextDisplay? = null

        private var health: Int = maxHealth

        private var lastHit: Long = 0

        private val protectedBlocks: LongOpenHashSet = LongOpenHashSet()

        companion object {
            private const val HIT_INTERVAL = 800
            private const val PROTECTED_RADIUS = 2
        }

        open fun canSpawn(playerLocation: Location): Boolean {
            val centralLocation = playerLocation.clone().add(0.0,2.0,0.0)
            val protectedCube = centralLocation.getCube(PROTECTED_RADIUS)

            centralLocation.subtract(0.0, 2.0, 0.0)
            var block = centralLocation.block
            var blocksToBedrock = 0
            val limit = getBuildingConfig().maxBlocksAboveBedrock
            while (block.type != Material.BEDROCK && blocksToBedrock < limit + 1) {
                blocksToBedrock++
                centralLocation.subtract(0.0, 1.0,0.0)
                block = centralLocation.block
            }

            return !protectedCube.any { it.block.type != Material.AIR } && blocksToBedrock <= limit
        }

        protected abstract fun getBuildingConfig(): BuildingConfig

        protected fun spawnDisplay(world: World, loc: Location, config: BuildingConfig): ItemDisplay {
            return world.spawn(loc, ItemDisplay::class.java) {
                it.setItemStack(config.displayItem)
                it.setTransformationMatrix(config.displayMatrix)
            }
        }

        protected fun spawnHitbox(world: World, loc: Location, config: BuildingConfig): Interaction {
            return world.spawn(loc.clone().add(config.hitboxLocModifier), Interaction::class.java) {
                it.isResponsive = true
                val dimensions = config.hitboxDimensions
                it.interactionHeight = dimensions.height
                it.interactionWidth = dimensions.width
            }
        }

        open fun spawn(builderPlayer: Player, spawnLocation: Location): Boolean {
            val world = spawnLocation.world
            val config = getBuildingConfig()

            val displayLoc = spawnLocation.clone().add(config.displayLocModifier)
            displayLoc.yaw = 0f
            displayLoc.pitch = 0f

            display = spawnDisplay(world, displayLoc, config)

            meleeHitbox = spawnHitbox(world, displayLoc, config)

            arrowHitbox =
                world.spawn(displayLoc.clone().add(config.hitboxLocModifier), ArmorStand::class.java) { stand ->
                    stand.isInvisible = true
                    stand.setGravity(false)
                    stand.isMarker = false
                }

            healthBar = world.spawn(displayLoc.clone().add(config.healthBarLocModifier), TextDisplay::class.java) {
                it.billboard = Display.Billboard.CENTER
                it.text(generateHealthBar().mm())
            }

            val nameLoc = displayLoc.clone().add(config.healthBarLocModifier).add(0.0, 0.4, 0.0)
            name = world.spawn(nameLoc, TextDisplay::class.java) {
                it.billboard = Display.Billboard.CENTER
                it.text(config.name.mm())
            }

            if (!config.customProtectedArea) {
                spawnLocation.clone().add(0.0,2.0,0.0).getCube(PROTECTED_RADIUS).forEach {
                    protectedBlocks.add(it.packCoordinates())
                }
            }

            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)

            return true
        }

        open fun remove() {
            display!!.remove()
            display = null

            meleeHitbox!!.remove()
            meleeHitbox = null

            arrowHitbox!!.remove()
            arrowHitbox = null

            healthBar!!.remove()
            healthBar = null

            name!!.remove()
            name = null
            protectedBlocks.clear()
            HandlerList.unregisterAll(this)
        }

        private fun generateHealthBar(): String {
            val builder = StringBuilder()

            val marks = 20
            val markSymbol = "❙"
            val healthPercentage = health * 100 / maxHealth
            val healthInMarks = healthPercentage * marks / 100

            builder.append("<dark_red>[<red>")

            for (i in 0 until marks) {
                if (i == healthInMarks) {
                    builder.append("<gray>")
                }

                builder.append(markSymbol)
            }

            builder.append("<dark_red>]")

            return builder.toString()
        }

        private fun takeDamage() {
            val now = System.currentTimeMillis()
            if (now - lastHit < HIT_INTERVAL) return
            lastHit = now

            health = max(health - 1, 0)
            healthBar!!.text(generateHealthBar().mm())

            if (health <= 0) {
                remove()
            }
        }

        @EventHandler
        fun onMeleeDamage(e: EntityDamageByEntityEvent) {
            if (meleeHitbox == null) return
            val hitEntity = e.entity
            if (hitEntity != meleeHitbox && hitEntity != arrowHitbox) return
            val damager = e.damager as? Player ?: return
            if (GameManager.getPlayerTeam(damager) == TeamType.DWARF) return

            takeDamage()

            if (hitEntity == arrowHitbox) {
                e.isCancelled = true
            }
        }

        @EventHandler
        fun onProjectileDamage(e: ProjectileHitEvent) {
            if (arrowHitbox == null) return
            if (e.hitEntity != arrowHitbox) return
            val shooter = e.entity.shooter as? Player ?: return
            if (GameManager.getPlayerTeam(shooter) == TeamType.DWARF) return

            takeDamage()
            e.entity.remove()
            shooter.playSound(shooter.location, Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f)
        }

        @EventHandler
        fun onBlockPlace(e: BlockPlaceEvent) {
            if (protectedBlocks.contains(e.blockPlaced.packCoordinates())) {
                e.isCancelled = true
            }
        }

    }

    private class MagicDoors(): Building(40) {

        private var unbreakableDoorDisplay: ItemDisplay? = null

        private var unbreakableDoorHitbox: Interaction? = null

        private var doorTeleportLocation: Location? = null

        private var unbreakableDoorTeleportLocation: Location? = null

        companion object {
            private val doorType = Material.OXIDIZED_COPPER_DOOR
            private val doorItem = createItem(doorType) {
                name = "<gray>Unbreakable Door"
                description = """
                    ?
                """
                persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
            }
        }

        override fun getBuildingConfig(): BuildingConfig = BuildingConfig(
            ItemStack(doorType),
            Matrix4f().scale(1.5f,2f,1.5f),
            Vector(0.0,1.0,0.0),
            HitboxDimensions(1f,2f),
            Vector(0.0,-1.0,0.0),
            Vector(0.0,1.0,0.0),
            -1,
            "<dark_aqua><b>Magic Doors"
        )

        override fun canSpawn(playerLocation: Location): Boolean {
            val blockAbove = playerLocation.clone().add(0.0, 1.0, 0.0).block
            return playerLocation.block.type == Material.AIR && blockAbove.type == Material.AIR
        }

        private fun canSpawnUnbreakableDoor(location: Location): Boolean {
            if (location.distanceSquared(display!!.location) > 10 * 10) return false

            val blockAbove = location.clone().add(0.0, 1.0, 0.0).block
            return location.block.type == doorType && blockAbove.type == doorType
        }

        override fun remove() {
            if (unbreakableDoorDisplay != null) {
                unbreakableDoorDisplay!!.remove()
                unbreakableDoorDisplay = null
            }

            if (unbreakableDoorHitbox != null) {
                unbreakableDoorHitbox!!.remove()
                unbreakableDoorHitbox = null
            }

            super.remove()
        }

        override fun spawn(builderPlayer: Player, spawnLocation: Location): Boolean {
            if (!super.spawn(builderPlayer, spawnLocation)) return false

            builderPlayer.inventory.addItem(doorItem)
            doorTeleportLocation = spawnLocation
            return true
        }

        @EventHandler
        fun onDoorPlace(e: BlockPlaceEvent) {
            if (unbreakableDoorDisplay != null) return
            val item = e.itemInHand
            if (item != doorItem) return

            val player = e.player
            val spawnLoc = e.blockPlaced.location
            if (!canSpawnUnbreakableDoor(spawnLoc)) {
                player.sendActionBar("<yellow>You cannot place it here!".mm())
                return
            }

            player.removeItem(item, 1)
            unbreakableDoorTeleportLocation = spawnLoc
            val config = getBuildingConfig()

            val displayLoc = spawnLoc.clone().add(config.displayLocModifier)
            val world = spawnLoc.world
            displayLoc.yaw = 0f
            displayLoc.pitch = 0f
            unbreakableDoorDisplay = spawnDisplay(world, displayLoc, config)
            unbreakableDoorHitbox = spawnHitbox(world, displayLoc, config)
        }


        @EventHandler
        fun onDoorClick(e: PlayerInteractEntityEvent) {
            if (unbreakableDoorDisplay == null) return
            val player = e.player
            if (GameManager.getPlayerTeam(player) == TeamType.ZOMBIE) return

            val clickedEntity = e.rightClicked
            val teleportLocation = when (clickedEntity) {
                unbreakableDoorHitbox -> doorTeleportLocation
                meleeHitbox -> unbreakableDoorTeleportLocation
                else -> null
            }

            if (teleportLocation != null) {
                player.playSound(player.location, Sound.BLOCK_IRON_DOOR_OPEN, 1f, 1f)
                player.teleport(teleportLocation)
            }
        }
    }

    private class SupplyBox(): Building(30) {

        private val dwarfCooldowns = CooldownUtil(SUPPLY_COOLDOWN * 1000L)

        companion object {
            private const val SUPPLY_COOLDOWN = 25

            private val availableSupplies: Array<ItemStack> = arrayOf(
                ItemStack(Material.MILK_BUCKET),
                ItemStack(Material.GOLDEN_APPLE, 3),
                createItem<PotionMeta>(Material.POTION) {
                    color = Color.RED

                    addCustomEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 3, false, false), true)
                    customName("<b><light_purple>Healing Potion".mm())
                },
                ItemStack(Material.SPECTRAL_ARROW, 32)
            )
        }

        override fun getBuildingConfig(): BuildingConfig = BuildingConfig(
            ItemStack(Material.BARREL),
            Matrix4f().scale(2f),
            Vector(0.0,1.0,0.0),
            HitboxDimensions(1.5f,1.5f),
            Vector(0.0,-0.5,0.0),
            Vector(0.0,1.5,0.0),
            10,
            "<light_purple><b>Supply Box"
        )

        private fun receiveRandomSupply(dwarf: Player) = dwarf.withCooldown(dwarfCooldowns) {
            val randomIdx = Random.nextInt(0, availableSupplies.size)
            inventory.addItem(availableSupplies[randomIdx])
            playSound(location, Sound.BLOCK_BARREL_OPEN, 1f, 1f)
        }

        @EventHandler
        fun onBoxClick(e: PlayerInteractEntityEvent) {
            if (meleeHitbox == null) return
            if (e.rightClicked != meleeHitbox) return
            val player = e.player
            if (GameManager.getPlayerTeam(player) == TeamType.ZOMBIE) return

            receiveRandomSupply(player)
        }

    }

    private class Ballista() : Building(20) {

        private var shootingTask: BukkitTask? = null

        override fun remove() {
            shootingTask!!.cancel()
            shootingTask = null
            super.remove()
        }

        override fun getBuildingConfig(): BuildingConfig {
            return BuildingConfig(
                ItemStack(Material.PAPER),
                Matrix4f().rotateY(Math.toRadians(90.0).toFloat()).scale(2.0f),
                Vector(0.0, 1.5, 0.0),
                HitboxDimensions(3f, 1.5f),
                Vector(0.0, -1.0, 0.0),
                Vector(0.0, 1.0, 0.0),
                15,
                "<red><b>Ballista"
            )
        }

        @Suppress("UnstableApiUsage")
        override fun spawn(builderPlayer: Player, spawnLocation: Location): Boolean {
            if (!super.spawn(builderPlayer, spawnLocation)) return false

            val location = display!!.location
            val world = location.world
            val locationAsVector = location.toVector()

            val range = 20.0
            val shootForce = 5f

            shootingTask = sync(period = TimeUnit.TICKS(20)) {

                for (e in location.getNearbyLivingEntities(range)) {
                    if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
                    else if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

                    val targetLoc = e.eyeLocation
                    val direction = targetLoc.toVector().subtract(locationAsVector).normalize()
                    val distance = targetLoc.distance(location)
                    if (distance <= 2) continue

                    val res = world.rayTraceBlocks(location, direction, distance, FluidCollisionMode.NEVER, true)
                    if (res != null) continue

                    display!!.lookAt(targetLoc, LookAnchor.EYES)
                    val arrow = world.spawnArrow(location, direction, shootForce, 0f)
                    arrow.shooter = builderPlayer

                    world.playSound(location, Sound.ENTITY_BREEZE_SHOOT, 1f, 1f)
                    break
                }

            }

            return true
        }

    }

    private class Mortar() : Building(10) {

        private var shootingTask: BukkitTask? = null

        companion object {
            private val flightParticles = Particle.END_ROD.builder().extra(0.0).count(2)
            private val rocketScale = Vector3f(1.5f, 1.5f, 1.5f)

            private const val PARTICLES_VIEW_RANGE = 40
            private const val ROCKET_DAMAGE = 12.0
        }

        override fun getBuildingConfig(): BuildingConfig {
            return BuildingConfig(
                ItemStack(Material.LEATHER),
                Matrix4f(),
                Vector(0.0, 0.5, 0.0),
                HitboxDimensions(1.5f, 1.5f),
                Vector(0.0, 0.0, 0.0),
                Vector(0.0, 2.0, 0.0),
                5,
                "<gold><b>Mortar"
            )
        }

        override fun spawn(builderPlayer: Player, spawnLocation: Location): Boolean {
            if (!super.spawn(builderPlayer, spawnLocation)) return false

            val maxTargets = 4
            val range = 15.0
            val targets = HashSet<LivingEntity>(maxTargets)
            val baseLoc = display!!.location.clone().add(0.0, 1.0, 0.0)

            shootingTask = sync(period = TimeUnit.SECONDS(8)) {
                for (e in baseLoc.getNearbyLivingEntities(range)) {
                    if (e is Player && GameManager.getPlayerTeam(e) == TeamType.DWARF) continue
                    else if (e !is Player && e.type != AIZombieScheduler.MOB_TYPE) continue

                    targets.add(e)
                    if (targets.size == maxTargets) {
                        break
                    }
                }

                val maxHorizontalOffset = 10.0
                for ((i, t) in targets.withIndex()) {
                    val xOffset = Random.nextDouble(-maxHorizontalOffset, maxHorizontalOffset)
                    val zOffset = Random.nextDouble(-maxHorizontalOffset, maxHorizontalOffset)
                    val middlePoint = baseLoc.clone().add(xOffset, 25.0, zOffset)

                    sync(delay = TimeUnit.TICKS(6 * i.toLong())) {
                        launchRocketWithAscend(baseLoc, middlePoint, t, builderPlayer)
                        baseLoc.world.playSound(baseLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f)
                    }
                }
                targets.clear()
            }

            return true
        }

        override fun remove() {
            shootingTask!!.cancel()
            shootingTask = null
            super.remove()
        }

        private fun spawnFireworkExplosion(location: Location, shooter: Player) {
            val world = location.world
            world.spawn(location, Firework::class.java) {
                it.shooter = shooter
                it.setGravity(false)
                val meta = it.fireworkMeta
                meta.addEffect(
                    FireworkEffect.builder()
                        .with(FireworkEffect.Type.BURST)
                        .withColor(Color.BLACK, Color.ORANGE)
                        .withFlicker()
                        .withTrail()
                        .build()
                )
                it.fireworkMeta = meta
            }.detonate()
        }

        private fun spawnRocketDisplay(location: Location, transformation: Transformation): ItemDisplay {
            val world = location.world
            return world.spawn(location, ItemDisplay::class.java) { display ->
                display.setItemStack(ItemStack(Material.FIREWORK_ROCKET))
                display.billboard = Display.Billboard.FIXED
                display.teleportDuration = 2
                display.interpolationDelay = 0
                display.interpolationDuration = 2
                display.transformation = transformation
            }
        }

        fun bezierInto(t: Double, out: Vector, start: Location, middle: Location, end: Location) {
            val u = 1 - t
            val x = u * u * start.x + 2 * u * t * middle.x + t * t * end.x
            val y = u * u * start.y + 2 * u * t * middle.y + t * t * end.y
            val z = u * u * start.z + 2 * u * t * middle.z + t * t * end.z
            out.x = x
            out.y = y
            out.z = z
        }

        private fun launchRocketWithAscend(
            startLocation: Location,
            intermediateLoc: Location,
            target: LivingEntity,
            shooter: Player
        ) {
            val world = startLocation.world

            val ascendDuration = TimeUnit.TICKS(10)
            val ascendTarget = startLocation.clone().add(0.0, 20.0, 0.0)

            val rocketLoc = Location(world, startLocation.x, startLocation.y, startLocation.z)
            val transformTranslation = Vector3f(0f, 0f, 0f)

            val rocket = spawnRocketDisplay(
                startLocation,
                Transformation(transformTranslation, Quaternionf(), rocketScale, Quaternionf())
            )

            var currTick = 0
            sync(period = TimeUnit.TICKS(1)) {
                if (currTick > ascendDuration || rocket.isDead) {
                    cancel()

                    if (rocket.isDead) return@sync

                    redirectRocket(
                        startLocation = rocketLoc.clone(),
                        intermediateLoc = intermediateLoc,
                        target = target,
                        shooter = shooter,
                        rocket = rocket
                    )
                    return@sync
                }

                val t = currTick.toDouble() / ascendDuration

                rocketLoc.x = startLocation.x
                rocketLoc.y = startLocation.y + (ascendTarget.y - startLocation.y) * t
                rocketLoc.z = startLocation.z

                rocket.teleport(rocketLoc)
                flightParticles.location(rocketLoc).receivers(PARTICLES_VIEW_RANGE, true).spawn()

                currTick++
            }
        }

        private fun redirectRocket(
            startLocation: Location,
            intermediateLoc: Location,
            target: LivingEntity,
            shooter: Player,
            rocket: ItemDisplay
        ) {
            val targetLoc = target.location
            val world = targetLoc.world

            val flightDuration = TimeUnit.SECONDS(1)

            val rocketLoc = Location(world, 0.0, 0.0, 0.0)
            val currentVec = Vector(0.0, 0.0, 0.0)
            val nextVec = Vector(0.0, 0.0, 0.0)
            val directionVec = Vector(0.0, 0.0, 0.0)

            val fromAxis = Vector3f(0f, 1f, 0f)
            val toAxis = Vector3f()
            val rotation = Quaternionf()
            val transformTranslation = Vector3f(0f, 0f, 0f)

            var currTick = 0
            sync(period = TimeUnit.TICKS(1)) {
                if (currTick > flightDuration || rocket.isDead || (rocketLoc.block.isSolid && currTick != 0)) {
                    rocket.remove()
                    spawnFireworkExplosion(rocketLoc, shooter)

                    cancel()
                    return@sync
                }

                val t = currTick.toDouble() / flightDuration
                val tNext = ((currTick + 1).toDouble() / flightDuration).coerceAtMost(1.0)

                bezierInto(t, currentVec, startLocation, intermediateLoc, targetLoc)
                bezierInto(tNext, nextVec, startLocation, intermediateLoc, targetLoc)

                directionVec.x = nextVec.x - currentVec.x
                directionVec.y = nextVec.y - currentVec.y
                directionVec.z = nextVec.z - currentVec.z

                if (directionVec.lengthSquared() > 1.0E-6) {
                    directionVec.normalize()
                    toAxis.set(directionVec.x.toFloat(), directionVec.y.toFloat(), directionVec.z.toFloat())
                    rotation.rotationTo(fromAxis, toAxis)

                    rocket.transformation =
                        Transformation(transformTranslation, rotation, rocketScale, Quaternionf())
                }

                rocketLoc.x = currentVec.x
                rocketLoc.y = currentVec.y
                rocketLoc.z = currentVec.z
                rocket.teleport(rocketLoc)
                flightParticles.location(rocketLoc).receivers(PARTICLES_VIEW_RANGE, true).spawn()

                currTick++
            }
        }

        @EventHandler
        fun onFireworkHit(e: EntityDamageByEntityEvent) {
            if (e.damager !is Firework) return
            e.damage = ROCKET_DAMAGE
        }

    }

}
