package com.jackob.dvz.kits.zombie.special

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler.Companion.UNPLACEABLE_KEY
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.*
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.watchers.WitchWatcher
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class Witch(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<WitchWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.WITCH) {
        isAggressive = true
    }

    override val aiZombieEnabled: Boolean = false

    private val magicCauldron = MagicCauldron(owner)

    companion object {
        private const val BREWING_COOLDOWN = 25

        private const val CAULDRON_COOLDOWN = 2

        private val brewingCooldowns = CooldownUtil(BREWING_COOLDOWN * 1000L)

        private val cauldronCooldowns = CooldownUtil(CAULDRON_COOLDOWN * 1000L)

        private val magicBrewingStand = createItem(Material.BREWING_STAND) {
            name = "<light_purple>Magic Brewing Stand"
            description = """
                Produces three random batches of potions
                Cooldown <gray>${BREWING_COOLDOWN}s
                <green>[Right] <white>- click to use
            """
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }

        private val magicCauldronItem = createItem(Material.CAULDRON) {
            name = "<light_purple>Magic Cauldron"
            description = """
                ?
            """
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }

        private const val BASE_POTION_AMPLIFIER = 0

        private val possibleBaseEffects = arrayOf(
            CustomPotion(
                PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, BASE_POTION_AMPLIFIER, false, false),
                Color.RED,
                "<red>Health"
            ),
            CustomPotion(
                PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, BASE_POTION_AMPLIFIER, false, false),
                Color.PURPLE,
                "<dark_purple>Damage"
            ),
            CustomPotion(
                PotionEffect(PotionEffectType.POISON, 16 * 20, BASE_POTION_AMPLIFIER, false, false),
                Color.LIME,
                "<green>Poison"
            ),
            CustomPotion(
                PotionEffect(PotionEffectType.NAUSEA, 16 * 20, BASE_POTION_AMPLIFIER, false, false),
                Color.GREEN,
                "<dark_green>Nausea"
            ),
            CustomPotion(
                PotionEffect(PotionEffectType.WEAKNESS, 16 * 20, BASE_POTION_AMPLIFIER, false, false),
                Color.BLACK,
                "<gray>Weakness"
            ),
            CustomPotion(
                PotionEffect(PotionEffectType.SLOWNESS, 16 * 20, BASE_POTION_AMPLIFIER, false, false),
                Color.AQUA,
                "<aqua>Slowness"
            ),
        )
    }

    init {
        WitchListener
    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!
        startDisguise(player)

        player.inventory.addItem(magicBrewingStand, magicCauldronItem, *createStartingPotions())
        player.playSound(player.location, Sound.ENTITY_WITCH_AMBIENT, 1f, 1f)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!
        stopDisguise(player)

        brewingCooldowns.removeFromCooldown(player)
        magicCauldron.unregisterCauldron()
    }

    private fun createStartingPotions(): Array<ItemStack> {
        val amount = 10
        return Array(possibleBaseEffects.size) { i ->
            val customPotion = possibleBaseEffects[i]
            createItem<PotionMeta>(Material.SPLASH_POTION, amount) {
                color = customPotion.color

                addCustomEffect(customPotion.potion, true)
                customName(customPotion.name.mm())
                setMaxStackSize(amount)
            }
        }
    }

    private fun brewPotions(witchPlayer: Player) = witchPlayer.withCooldown(brewingCooldowns) {
        val randomPicks = Array(3) {
            Random.nextInt(possibleBaseEffects.size)
        }
        val amount = 16

        for (idx in randomPicks) {
            val customPotion = possibleBaseEffects[idx]
            inventory.addItem(createItem<PotionMeta>(Material.SPLASH_POTION, amount) {
                color = customPotion.color

                addCustomEffect(customPotion.potion, true)
                customName(customPotion.name.mm())
                setMaxStackSize(amount)
            })
        }

        playSound(location, Sound.BLOCK_BREWING_STAND_BREW, 1f, 1f)
    }

    private fun spawnCauldron(witchPlayer: Player, clickedBlock: Block?) = witchPlayer.withCooldown(cauldronCooldowns) {
        val location = clickedBlock?.location?.add(0.0, 1.0, 0.0) ?: return@withCooldown
        magicCauldron.spawnCauldron(location)
    }

    object WitchListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onItemClick(e: PlayerInteractEvent) {
            val player = e.player
            val witchKit = KitsManager.getKit(player) as? Witch ?: return

            val rightClickedItem = e.rightClickItem
            if (rightClickedItem == magicBrewingStand) {
                witchKit.brewPotions(player)
            } else if (rightClickedItem == magicCauldronItem) {
                witchKit.spawnCauldron(player, e.clickedBlock)
            }
        }
    }

    private class MagicCauldron(val owner: UUID) : Listener {

        private var display: BlockDisplay? = null

        private var hitbox: Interaction? = null

        private var healthBar: TextDisplay? = null

        private var health: Int = 0

        private var lastHit: Long = 0

        private var level: Int = 0

        companion object {
            private const val MAX_HEALTH = 10

            private const val HIT_INTERVAL = 1500

            private const val MAX_LEVEL = 30

            private const val MIN_ENEMY_DISTANCE = 10

            private const val FILLING_RATE = 5

            private const val FIRST_LEVEL_CAP = 10

            private var deathPotionContents = arrayOf(
                PotionEffect(PotionEffectType.WITHER, 12 * 20, 3, false, false),
                PotionEffect(PotionEffectType.DARKNESS, 15 * 20, 2, false, false),
                PotionEffect(PotionEffectType.MINING_FATIGUE, 20 * 20, 1, false, false),
            )
        }

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        fun spawnCauldron(location: Location) {
            removeCauldron()

            health = MAX_HEALTH

            display = spawnEmptyCauldron(location)
            hitbox = spawnHitbox(location.clone().add(0.5, 0.0, 0.5))
            healthBar = spawnHealthBar(location.clone().add(0.5, 1.5, 0.5))
        }

        fun unregisterCauldron() {
            HandlerList.unregisterAll(this)
            removeCauldron()
        }

        private fun removeCauldron() {
            if (display == null) return

            display!!.remove()
            display = null

            hitbox!!.remove()
            hitbox = null

            healthBar!!.remove()
            healthBar = null
            health = 0
            level = 0
        }

        private fun spawnHitbox(location: Location): Interaction {
            return location.world.spawn<Interaction>(location, Interaction::class.java) { interaction ->
                interaction.isResponsive = true
            }
        }

        private fun spawnHealthBar(location: Location): TextDisplay {
            return location.world.spawn(location, TextDisplay::class.java) { display ->
                display.text("<green>$health/$MAX_HEALTH".mm())
                display.billboard = Display.Billboard.CENTER
            }
        }

        private fun spawnEmptyCauldron(location: Location): BlockDisplay {
            val cauldronData = Material.CAULDRON.createBlockData()

            return location.world.spawn(location, BlockDisplay::class.java) { display ->
                display.block = cauldronData
            }
        }

        private fun spawnFilledCauldron(location: Location): BlockDisplay {
            val cauldronData = Material.WATER_CAULDRON.createBlockData() as Levelled
            cauldronData.level = level / 10

            return location.world.spawn(location, BlockDisplay::class.java) { display ->
                display.block = cauldronData
            }
        }

        private fun takeDamage(damager: Player) {
            if (!Tag.ITEMS_PICKAXES.isTagged(damager.inventory.itemInMainHand.type)) return

            val now = System.currentTimeMillis()
            if (now - lastHit < HIT_INTERVAL) return
            lastHit = now

            health--
            healthBar!!.text("<green>$health/$MAX_HEALTH".mm())
            rewardDamager(damager)

            if (health <= 0) {
                removeCauldron()
            }
        }

        private fun drainCauldron(amount: Int) {
            level = max(level - amount, 0)

            val displayLoc = display!!.location

            display!!.remove()
            display = if (level < FIRST_LEVEL_CAP) {
                spawnEmptyCauldron(displayLoc)
            } else {
                spawnFilledCauldron(displayLoc)
            }
        }

        private fun producePotions(witchPlayer: Player) {
            if (level < FIRST_LEVEL_CAP) return

            val potions = level / FIRST_LEVEL_CAP * 2

            val deathPotion = createItem<PotionMeta>(Material.SPLASH_POTION, potions) {
                color = Color.MAROON

                for (e in deathPotionContents) {
                    addCustomEffect(e, true)
                }
                customName("<b><dark_red>Potion of Death".mm())
                setMaxStackSize(potions)
            }

            witchPlayer.inventory.addItem(deathPotion)
            witchPlayer.playSound(witchPlayer.location, Sound.ENTITY_WITCH_DRINK, 1f, 1f)
            drainCauldron(MAX_LEVEL)
        }

        private fun rewardDamager(damager: Player) {
            if (level == 0) return

            if (level >= FIRST_LEVEL_CAP) {
                damager.addPotionEffect(PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0, false, false))
            }
            drainCauldron(FILLING_RATE)
        }

        private fun fillCauldron(victim: Player, witchPlayer: Player) {
            if (level == MAX_LEVEL) return

            val displayLoc = display!!.location
            if (victim.location.distanceSquared(displayLoc) > MIN_ENEMY_DISTANCE * MIN_ENEMY_DISTANCE) return

            level = min(level + FILLING_RATE, MAX_LEVEL)

            witchPlayer.playSound(witchPlayer.location, Sound.ENTITY_WITCH_CELEBRATE, 1f, 1f)
            Particle.TRAIL.builder()
                .location(victim.location.add(0.0, 1.0, 0.0))
                .offset(1.0, 1.0, 1.0)
                .count(8)
                .data(Particle.Trail(displayLoc.clone().add(0.0, 0.5, 0.0), Color.MAROON, 35))
                .receivers(16, true)
                .spawn()

            if (level < FIRST_LEVEL_CAP) return

            display!!.remove()
            display = spawnFilledCauldron(displayLoc)
        }

        @EventHandler
        fun onHitboxHit(e: EntityDamageByEntityEvent) {
            if (display == null) return

            if (e.entity != hitbox) return
            val damager = e.damager as? Player ?: return
            if (GameManager.getPlayerTeam(damager) != TeamType.DWARF) return

            takeDamage(damager)
        }

        @EventHandler
        fun onPotionSplash(e: PotionSplashEvent) {
            if (display == null) return

            if (!e.potion.effects.any { it.type == PotionEffectType.INSTANT_DAMAGE }) return

            val shooter = e.potion.shooter as? Player ?: return
            if (shooter.uniqueId != owner) return

            for (enemy in e.affectedEntities) {
                if (e.getIntensity(enemy) == 0.0) continue
                val victim = enemy as? Player ?: continue

                fillCauldron(victim, shooter)
            }
        }

        @EventHandler
        fun onInteractionRightClick(e: PlayerInteractEntityEvent) {
            if (display == null) return
            if (e.rightClicked != hitbox) return

            val witchPlayer = e.player
            if (witchPlayer.uniqueId != owner) return

            producePotions(witchPlayer)
        }
    }

    data class CustomPotion(val potion: PotionEffect, val color: Color, val name: String)
}
