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
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class Spider(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createMobDisguise(DisguiseType.CAVE_SPIDER) { }

    override val aiZombieEnabled: Boolean = false

    private val venomProjectiles = HashSet<UUID>(8)

    companion object {
        private val biteItem = Material.SPIDER_EYE

        private val bitePoisonEffect = PotionEffect(PotionEffectType.POISON, 6 * 20, 0, true, true)

        private val biteNauseaEffect = PotionEffect(PotionEffectType.NAUSEA, 6 * 20, 0, true, true)

        private val venomEffect = PotionEffect(PotionEffectType.POISON, 10 * 20, 2, true, true)

        private const val VENOM_COOLDOWN = 8

        private val venomCooldowns = CooldownUtil(VENOM_COOLDOWN* 1000L)

        private val venomItem = createItem(Material.SLIME_BLOCK) {
            name = "<green>Spider's venom"
            description = """
                Spits poisonous venom which weakens walls and poisons dwarves
                Cooldown <gray>${VENOM_COOLDOWN}s
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
        player.inventory.addItem(venomItem)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        stopDisguise(ownerId.toPlayer()!!)
    }

    private fun bitePassiveAbility(dwarfVictim: Player) {
        dwarfVictim.addPotionEffect(biteNauseaEffect)
        dwarfVictim.addPotionEffect(bitePoisonEffect)
    }

    private fun venomAbility(spiderPlayer: Player) = spiderPlayer.withCooldown(venomCooldowns) {
        val maxLaunches = 8
        val slimeBall = ItemStack(Material.SLIME_BALL)
        val period = TimeUnit.TICKS(5)

        var launches = maxLaunches
        sync(period = period) {
            val vector = eyeLocation.direction.normalize().multiply(1.3)
            val slimeProjectile: Snowball = launchProjectile(Snowball::class.java, vector)
            slimeProjectile.item = slimeBall
            venomProjectiles.add(slimeProjectile.uniqueId)

            launches--

            if (launches <= 0) {
                cancel()
            }
        }

        addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, (period * maxLaunches).toInt(), 1, false, false))
        playSound(location, Sound.ENTITY_SPIDER_AMBIENT, 1f, 1f)
    }

    @Suppress("UnstableApiUsage")
    private fun venomOnHit(projectileID: UUID, victim: Entity?, hitBlock: Block?, shooter: Player) {
        if (!venomProjectiles.remove(projectileID)) return
        val poisonedBlock = Material.GREEN_WOOL

        if (victim != null) {
            val dwarfVictim = victim as? Player ?: return
            if (GameManager.getPlayerTeam(dwarfVictim) != TeamType.DWARF) return

            victim.addPotionEffect(venomEffect)
        } else if (hitBlock != null && hitBlock.type != poisonedBlock && hitBlock.type != Material.BEDROCK) {
            val blockBreakSimulate = BlockBreakEvent(hitBlock, shooter)
            Bukkit.getPluginManager().callEvent(blockBreakSimulate)

            if (!blockBreakSimulate.isCancelled) {
                hitBlock.type = poisonedBlock
            }
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
            if (rightClickedItem != venomItem) return

            spiderKit.venomAbility(player)
        }

        @EventHandler
        fun onProjectileHit(e: ProjectileHitEvent) {
            val shooter = e.entity.shooter as? Player ?: return
            val spiderKit = KitsManager.getKit(shooter) as? Spider ?: return

            spiderKit.venomOnHit(e.entity.uniqueId, e.hitEntity, e.hitBlock, shooter)
        }

    }

}
