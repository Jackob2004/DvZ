package com.jackob.dvz.kits.dwarf.hero

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler.Companion.UNPLACEABLE_KEY
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.*
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MiscDisguise
import me.libraryaddict.disguise.disguisetypes.watchers.FallingBlockWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.util.*

class Elf(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createPlayerDisguise("elf", "Mirkwood Elf") {
        setItemStack(EquipmentSlot.HEAD, hiddenArmorPiece)
        setItemStack(EquipmentSlot.CHEST, hiddenArmorPiece)
        setItemStack(EquipmentSlot.LEGS, hiddenArmorPiece)
        setItemStack(EquipmentSlot.FEET, hiddenArmorPiece)
    }

    private var disguiseTask: BukkitTask? = null

    private var currDisguise: Int = 0

    companion object {
        private const val DISGUISE_COOLDOWN = 25

        private const val DISGUISE_DURATION = 15

        private val hiddenArmorPiece = ItemStack(Material.AIR)

        private val disguiseCooldowns = CooldownUtil(DISGUISE_COOLDOWN * 1000L)

        private val DISGUISE_ABILITY_ITEM = createItem(Material.OAK_LEAVES) {
            name = "<green><u>Forest Disguise"
            description = """
                  
                  Empowers you with the ability to appear as leaves, a flower or grass
                  You can stay in hiding for $DISGUISE_DURATION seconds
                  Any movement will transform you back to visible form
                  Cooldown $DISGUISE_COOLDOWN seconds
                  <green>[Right] <white>- click to activate
                  <green>[Left] <white>- click to cycle through disguises 
            """
            enchant(Enchantment.UNBREAKING, 10)
            persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
        }

        private val forestDisguises: Array<Disguise> = Array(ForestDisguise.entries.size) { i ->
            createForestDisguise(ForestDisguise.entries[i])
        }

        private fun createForestDisguise(forestDisguise: ForestDisguise): Disguise {
            val disguise = MiscDisguise(DisguiseType.FALLING_BLOCK)
            disguise.setViewSelfDisguise(false)

            val watcher = disguise.watcher as FallingBlockWatcher
            watcher.blockData = forestDisguise.type.createBlockData()

            return disguise
        }

        private enum class ForestDisguise(val type: Material, val disguiseName: String) {
            LEAVES(Material.OAK_LEAVES, "<green>Leaves"),
            GRASS(Material.SHORT_GRASS, "<dark_green>Grass"),
            FLOWER(Material.BLUE_ORCHID, "<blue>Flower"),
        }

    }

    init {
        ElfListener
    }

    override fun onActivate() {
        super.onActivate()

        val player = ownerId.toPlayer()!!
        startDisguise(player)
        player.inventory.addItem(DISGUISE_ABILITY_ITEM)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!

        stopDisguise(player)
        deactivateDisguiseAbility(player, false)
    }

    private fun nextDisguise(player: Player) {
        currDisguise = (currDisguise + 1) % forestDisguises.size

        player.sendActionBar("<white>Current Disguise: ${ForestDisguise.entries[currDisguise].disguiseName}".mm())
        player.playSound(player.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)
    }

    private fun activateDisguiseAbility(player: Player) = player.withCooldown(disguiseCooldowns) {
        if (disguiseTask != null) return

        stopDisguise(this)
        startDisguise(this, forestDisguises[currDisguise])
        addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, Int.MAX_VALUE, Int.MAX_VALUE))

        val lastLocation = location

        var timer = DISGUISE_DURATION
        disguiseTask = sync(period = TimeUnit.SECONDS(1)) {
            timer--

            val hasMoved = lastLocation.distanceSquared(location) > 1

            if (timer <= 0 || hasMoved) {
                deactivateDisguiseAbility(player, true)
                return@sync
            }
            player.sendActionBar("<green>Forest disguise active".mm())
        }

        playSound(lastLocation, Sound.BLOCK_CHERRY_LEAVES_FALL, 1f, 1f)
    }

    private fun deactivateDisguiseAbility(player: Player, startBaseDisguise: Boolean) {
        if (disguiseTask == null) return
        disguiseTask!!.cancel()
        disguiseTask = null

        stopDisguise(player)
        if (startBaseDisguise) {
            startDisguise(player)
            player.removePotionEffect(PotionEffectType.INVISIBILITY)
            player.sendActionBar("<red>Leaf disguise deactivated".mm())
            player.playSound(player.location, Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1f)
        }
    }

    object ElfListener : Listener {
        init {
            DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        }

        @EventHandler
        fun onItemClick(event: PlayerInteractEvent) {
            val player = event.player
            val elfKit = KitsManager.getKit(player) as? Elf ?: return

            val rightClickedItem = event.rightClickItem
            val leftClickedItem = event.leftClickItem

            if (rightClickedItem == DISGUISE_ABILITY_ITEM) {
                elfKit.activateDisguiseAbility(player)
            } else if (leftClickedItem == DISGUISE_ABILITY_ITEM) {
                elfKit.nextDisguise(player)
            }
        }

        @EventHandler
        fun onPlayerQuit(event: PlayerQuitEvent) {
            val player = event.player
            val elfKit = KitsManager.getKit(player) as? Elf ?: return

            elfKit.deactivateDisguiseAbility(player, false)
        }
    }
}
