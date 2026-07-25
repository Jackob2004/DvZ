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
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.random.Random

class Spider(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CAVE_SPIDER) { }

    override val aiZombieEnabled: Boolean = true

    private val venomProjectiles = HashSet<UUID>(8)

    private val webProjectiles = HashSet<UUID>(8)

    companion object {
        private val biteItem = Material.SPIDER_EYE

        private val bitePoisonEffect = PotionEffect(PotionEffectType.POISON, 6 * 20, 0, true, true)

        private val biteNauseaEffect = PotionEffect(PotionEffectType.NAUSEA, 6 * 20, 0, true, true)

        private val venomEffect = PotionEffect(PotionEffectType.POISON, 10 * 20, 2, true, true)

        private const val VENOM_COOLDOWN = 8

        private const val WEB_COOLDOWN = 10

        private val venomCooldowns = CooldownUtil(VENOM_COOLDOWN * 1000L)

        private val webCooldowns = CooldownUtil(WEB_COOLDOWN * 1000L)

        private val venomItem = createItem(Material.SLIME_BLOCK) {
            name = "<green>Spider's venom"
            description = """
                Spits poisonous venom which weakens walls and poisons dwarves
                Cooldown <gray>${VENOM_COOLDOWN}s
                <green>[Right] <white>- click to use the ability
            """
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }

        private val cobwebItem = createItem(Material.COBWEB) {
            name = "<green>Spider's web"
            description = """
                Launches cobweb which traps dwarves on hit
                Cooldown <gray>${WEB_COOLDOWN}s
                <green>[Right] <white>- click to use the ability
            """
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }
    }

    init {
        SpiderListener
    }

    override fun onActivate() {
        super.onActivate()
        val player = ownerId.toPlayer()!!

        startDisguise(player)
        player.inventory.addItem(venomItem, cobwebItem)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)

        venomProjectiles.clear()
        webProjectiles.clear()
    }

    private fun bitePassiveAbility(dwarfVictim: Player) {
        dwarfVictim.addPotionEffect(biteNauseaEffect)
        dwarfVictim.addPotionEffect(bitePoisonEffect)
    }

    private fun launchProjectiles(
        projectileItem: ItemStack?,
        shooter: Player,
        projectilePool: HashSet<UUID>,
        sound: Sound
    ) {
        val maxLaunches = 8
        val period = TimeUnit.TICKS(5)

        var launches = maxLaunches
        sync(period = period) {
            val vector = shooter.eyeLocation.direction.normalize().multiply(1.3)
            val slimeProjectile: Snowball = shooter.launchProjectile(Snowball::class.java, vector)
            if (projectileItem != null) {
                slimeProjectile.item = projectileItem
            }
            projectilePool.add(slimeProjectile.uniqueId)

            launches--

            if (launches <= 0) {
                cancel()
            }
        }

        shooter.addPotionEffect(
            PotionEffect(
                PotionEffectType.SLOWNESS,
                (period * maxLaunches).toInt(),
                1,
                false,
                false
            )
        )
        shooter.playSound(shooter.location, sound, 1f, 1f)
    }

    private fun venomAbility(spiderPlayer: Player) = spiderPlayer.withCooldown(venomCooldowns) {
        launchProjectiles(ItemStack(Material.SLIME_BALL), this, venomProjectiles, Sound.ENTITY_SPIDER_AMBIENT)
    }

    private fun cobwebAbility(spiderPlayer: Player) = spiderPlayer.withCooldown(webCooldowns) {
        launchProjectiles(null, this, webProjectiles, Sound.ENTITY_SPIDER_STEP)
    }

    private fun handleProjectileHit(projectileID: UUID, victim: Entity?, hitBlock: Block?, shooter: Player) {
        if (venomProjectiles.remove(projectileID)) {
            onVenomHit(victim, hitBlock, shooter)
        } else if (webProjectiles.remove(projectileID)) {
            onCobwebHit(victim, shooter)
        }
    }

    private fun onVenomHit(victim: Entity?, hitBlock: Block?, shooter: Player) {
        val poisonedBlock = Material.GREEN_WOOL

        if (victim != null) {
            val dwarfVictim = victim as? Player ?: return
            if (GameManager.getPlayerTeam(dwarfVictim) != TeamType.DWARF) return

            victim.addPotionEffect(venomEffect)
        } else if (hitBlock != null && hitBlock.type != poisonedBlock && hitBlock.isBreakable(shooter)) {
            hitBlock.type = poisonedBlock
        }
    }

    private fun onCobwebHit(victim: Entity?, shooter: Player) {
        val dwarfVictim = victim as? Player ?: return
        if (GameManager.getPlayerTeam(dwarfVictim) != TeamType.DWARF) return

        val spiderWeb = Material.COBWEB

        val block = if (Random.nextInt(2) == 1) {
            dwarfVictim.location.block
        } else {
            dwarfVictim.eyeLocation.block
        }

        if (block.type != spiderWeb && block.isBreakable(shooter)) {
            block.type = spiderWeb
        }
    }

    object SpiderListener : Listener {

        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onDwarfHit(e: EntityDamageByEntityEvent) {
            val spiderPlayer = e.damager as? Player ?: return
            val spiderKit = KitsManager.getKit(spiderPlayer) as? Spider ?: return
            if (spiderPlayer.inventory.itemInMainHand.type != biteItem) return

            val dwarfVictim = e.entity as? Player ?: return
            if (GameManager.getPlayerTeam(dwarfVictim) != TeamType.DWARF) return

            spiderKit.bitePassiveAbility(dwarfVictim)
        }

        @EventHandler
        fun onItemClick(e: PlayerInteractEvent) {
            val player = e.player
            val spiderKit = KitsManager.getKit(player) as? Spider ?: return

            val rightClickedItem = e.rightClickItem
            if (rightClickedItem == venomItem) {
                spiderKit.venomAbility(player)
            } else if (rightClickedItem == cobwebItem) {
                spiderKit.cobwebAbility(player)
            }
        }

        @EventHandler
        fun onProjectileHit(e: ProjectileHitEvent) {
            val shooter = e.entity.shooter as? Player ?: return
            val spiderKit = KitsManager.getKit(shooter) as? Spider ?: return

            spiderKit.handleProjectileHit(e.entity.uniqueId, e.hitEntity, e.hitBlock, shooter)
        }

    }

}
