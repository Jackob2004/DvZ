package com.jackob.dvz.core.handlers

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.equipment.CustomItemType
import com.jackob.dvz.core.equipment.EquipmentRegister
import com.jackob.dvz.core.events.DwarfGoldCollectEvent
import com.jackob.dvz.core.objects.AIZombieScheduler
import com.jackob.dvz.core.objects.DarknessManager
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.ObtainableRegistry
import com.jackob.dvz.storage.loadConfig
import com.jackob.dvz.storage.toItemStack
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.name
import com.jackob.dvz.util.removeItem
import com.jackob.dvz.util.toSound
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import kotlin.random.Random

private const val EQUIPMENT_COOLDOWN = 500L

/**
 * Handles gameplay mechanics that are shared across multiple game states
 */
class GameplayMechanicsHandler : CoreHandler {

    private val equipmentCooldowns = CooldownUtil(EQUIPMENT_COOLDOWN)

    private val obtainables: Map<Material, Pair<ItemStack, Sound>> =
        loadConfig<ObtainableRegistry>("obtainable_config.yml")!!.obtainables.associateTo(
            HashMap()
        ) {
            Material.matchMaterial(it.rackType)!! to Pair(it.item.toItemStack(), it.pickUpSound.toSound()!!)
        }

    companion object {
        val UNPLACEABLE_KEY = NamespacedKey(DvZ.INSTANCE, "unplaceable")

        private val negativeEffects = arrayOf(
            PotionEffectType.POISON,
            PotionEffectType.NAUSEA,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
        )
    }

    private fun handleObtainableItems(clickedBlock: Material): Pair<ItemStack, Sound>? {
        return obtainables[clickedBlock]
    }

    @EventHandler
    fun onPotionBreak(e: PotionSplashEvent) {
        if (!e.potion.effects.any { it.type in negativeEffects }) return

        val shooter = e.entity.shooter as? Player ?: return
        val shooterTeam = GameManager.getPlayerTeam(shooter) ?: return

        for (entity in e.affectedEntities) {
            if (shooterTeam == TeamType.ZOMBIE && entity.type == AIZombieScheduler.MOB_TYPE) {
                e.setIntensity(entity, 0.0)
                continue
            }

            val victim = entity as? Player ?: continue
            if (GameManager.getPlayerTeam(victim) == shooterTeam) {
                e.setIntensity(victim, 0.0)
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        event.expToDrop = 0

        if (GameManager.getPlayerTeam(player) != TeamType.DWARF) return

        val blockType = event.block.type
        if (blockType.name.endsWith("_ORE") || blockType == Material.GRAVEL) {
            event.isDropItems = false
            val goldDropBase = ConfigStorage.GOLD_COLLECT_BASELINE
            val goldAmount = if (blockType == Material.GRAVEL) 1 else Random.nextInt(goldDropBase, goldDropBase * 2)
            Bukkit.getPluginManager().callEvent(DwarfGoldCollectEvent(player, goldAmount))
        }

        if (blockType == Material.GRAVEL) {
            player.inventory.addItem(ItemStack(Material.COBBLESTONE))
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.itemInHand.persistentDataContainer.has(UNPLACEABLE_KEY)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemCraft(event: CraftItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onAnvilUsage(event: PrepareAnvilEvent) {
        event.result = null
    }

    @EventHandler
    fun onItemEnchant(event: PrepareItemEnchantEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onWoodStrip(event: EntityChangeBlockEvent) {
        if (event.entity !is Player) return
        if (!Tag.LOGS.isTagged(event.block.type)) return
        event.isCancelled = true
    }

    @EventHandler
    fun onRightClickBlock(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        if (GameManager.getPlayerTeam(player) != TeamType.DWARF) return
        if (event.hand != EquipmentSlot.HAND) return
        if (equipmentCooldowns.isOnCooldown(player)) return

        val itemInHand = if (event.item == null) Material.AIR else event.item!!.type
        val clickedBlock = event.clickedBlock!!.type

        var soundEffect: Sound
        var item: ItemStack

        when (itemInHand) {
            // woodworking
            Material.IRON_AXE if (Tag.LOGS.isTagged(clickedBlock)) -> {
                item = createItem(Material.DARK_OAK_LOG) {
                    name = "<gray>Log"
                    description = """
                      <white> [Right]<gray> click on saw to turn them into planks
                """
                    persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
                }
                soundEffect = Sound.BLOCK_WOOD_BREAK
            }

            Material.DARK_OAK_LOG if (clickedBlock == Material.IRON_BARS) -> {
                item = createItem(Material.LEATHER) {
                    name = "<gray>Planks"
                    description = """
                      <white> [Right]<gray> click on saw to turn them into sticks
                """
                }
                soundEffect = Sound.UI_STONECUTTER_TAKE_RESULT
            }

            Material.LEATHER if (clickedBlock == Material.IRON_BARS) -> {
                item = createItem(Material.STICK, 3) {
                    name = "<gray>Stick"
                    description = """
                      <white> [Right]<gray> click on saw to turn them into bowls
                      <gray>  or click on oil to turn them into torches
                """
                }
                soundEffect = Sound.UI_STONECUTTER_TAKE_RESULT
            }

            Material.STICK if (clickedBlock == Material.IRON_BARS) -> {
                item = createItem(Material.GRAY_DYE, 3) {
                    name = "<gray>Bowls"
                    description = """
                      <white> [Right]<gray> click on oil to turn them into mortal
                """
                }
                soundEffect = Sound.UI_STONECUTTER_TAKE_RESULT
            }

            Material.STICK if (clickedBlock == Material.SPONGE) -> {
                item = createItem(Material.TORCH, 3) {
                    name = "<green>Torch"
                    description = """
                      <gray> Place to see in the darkness
                """
                    persistentDataContainer.set(DarknessManager.RADIANCE, PersistentDataType.BOOLEAN, true)
                }
                soundEffect = Sound.BLOCK_FIRE_AMBIENT
            }

            Material.GRAY_DYE if (clickedBlock == Material.SPONGE) -> {
                item = EquipmentRegister.getItem(CustomItemType.MORTAR)!!
                soundEffect = Sound.BLOCK_STONE_HIT
            }

            Material.AIR -> {
                val obtainable = handleObtainableItems(clickedBlock) ?: return
                item = obtainable.first
                soundEffect = obtainable.second
            }

            else -> return
        }

        event.isCancelled = true
        event.item?.let {
            if (it.type != Material.IRON_AXE)
                player.removeItem(it, 1)
        }
        player.playSound(player.location, soundEffect, 1f, 1f)
        player.inventory.addItem(item)
    }
}