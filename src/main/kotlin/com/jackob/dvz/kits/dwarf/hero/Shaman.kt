package com.jackob.dvz.kits.dwarf.hero

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.util.CombinationUtil
import com.jackob.dvz.util.CombinationUtil.ClickType.*
import com.jackob.dvz.util.CombinationUtil.Sequence
import com.jackob.dvz.util.ManaUtil
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.playCircleEffect
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.withMana
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.sqrt

class Shaman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createPlayerDisguise("shaman", "Shaman") {
        setItemStack(EquipmentSlot.HEAD, hiddenArmorPiece)
        setItemStack(EquipmentSlot.CHEST, hiddenArmorPiece)
        setItemStack(EquipmentSlot.LEGS, hiddenArmorPiece)
        setItemStack(EquipmentSlot.FEET, hiddenArmorPiece)
    }

    private val manaBank = ManaUtil(owner, 100, SHAMAN_STAFF)

    private val staffCombinations = CombinationUtil(owner, SHAMAN_STAFF).apply {
        registerAction(Sequence(RIGHT, LEFT, RIGHT), ::spawnTotem)
        registerAction(Sequence(RIGHT, RIGHT, RIGHT), ::leapTowardsTotem)
        registerAction(Sequence(RIGHT, LEFT, LEFT), ::pushEnemies)
        registerAction(Sequence(RIGHT, RIGHT, LEFT), ::pullEnemies)
    }

    private var totem: ArmorStand? = null

    private var totemInfoBar: TextDisplay? = null

    private var totemTickingTask: BukkitTask? = null

    companion object {
        private val hiddenArmorPiece = ItemStack(Material.AIR)
        private val SHAMAN_STAFF = NamespacedKey(DvZ.INSTANCE, "shaman-staff-item")

        private val shamanStaff = createItem(Material.WOODEN_HOE) {
            name = "<dark_green>Shaman Staff"
            description = """
                ? 
            """
            persistentDataContainer.set(ManaUtil.MANA_ITEM, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(SHAMAN_STAFF, PersistentDataType.BOOLEAN, true)
        }

        private val totemPositiveEffect =  PotionEffect(PotionEffectType.REGENERATION, 2 * 20, 0, false, false)
        private val defaultParticles = Particle.ENCHANT.builder().count(1).extra(0.0)
        private val pushParticles = Particle.BUBBLE_POP.builder().count(1).extra(0.0)

        private const val TOTEM_RANGE = 10.0
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
    }

    private fun removeTotem() {
        if (totem != null) {
            totem!!.remove()
            totem = null
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
        location.add(0.0,0.8,0.0).playCircleEffect(defaultParticles, effectRange, effectRange + 5)
    }

    private fun spawnTotem(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, 500) {
        removeTotem()

        val vector = shamanPlayer.eyeLocation.direction.normalize().multiply(1.2)
        vector.y = 1.2

        val maxLifeTime = 30
        var countDown = maxLifeTime

        totemInfoBar = world.spawn(eyeLocation, TextDisplay::class.java) {
            it.billboard = Display.Billboard.CENTER
            it.text("<light_purple>⌛<gray> ${maxLifeTime}s".mm())
        }

        totem = world.spawn(location.add(0.0, 1.0, 0.0), ArmorStand::class.java) {
            it.isInvulnerable = true
            it.velocity = vector
            it.addPassenger(totemInfoBar!!)
        }

        totemTickingTask = sync(delay = TimeUnit.SECONDS(2), period = TimeUnit.SECONDS(1)) {
            onTotemTick()
            totemInfoBar!!.text("<light_purple>⌛<gray> ${countDown}s".mm())
            countDown--

            if (countDown <= 0) {
                removeTotem()
            }
        }

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

        shamanPlayer.withMana(manaBank, 200) {
            val percentage = distance * 100 / maxDistance / 100
            val pullForce = 3.5 * percentage
            val vector = targetLocation.toVector().subtract(location.toVector()).normalize().multiply(pullForce)
            velocity = vector
        }
    }

    private fun playWaveEffect(location: Location) {
        var counter = 1
        val range = TOTEM_RANGE.toInt()
        sync(period = TimeUnit.TICKS(3)) {
            location.playCircleEffect(pushParticles, counter, range + 5)

            counter++
            if (counter == range) {
                cancel()
            }
        }
    }

    private fun pushEnemies(shamanPlayer: Player) {
        if (totem == null) return
        shamanPlayer.withMana(manaBank, 200) {
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
                playWaveEffect(totemLoc)

                counter--
                if (counter <= 0) {
                    cancel()
                }
            }

        }
    }

    private fun pullEnemies(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, 100) {
        sendMessage("Pulling enemies")
    }

}