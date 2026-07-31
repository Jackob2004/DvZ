package com.jackob.dvz.core.equipment

import com.jackob.dvz.core.handlers.GameplayMechanicsHandler.Companion.UNPLACEABLE_KEY
import com.jackob.dvz.util.CooldownUtil
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.enchant
import com.jackob.dvz.util.name
import com.jackob.dvz.util.removeItem
import com.jackob.dvz.util.rightClickItem
import com.jackob.dvz.util.withCooldown
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

private const val LEAF_USE_COOLDOWN = 2

private const val REGEN_DURATION = 3

private const val REGEN_STRENGTH = 2

class PurifyingLeaf : CustomItem(), Listener {
    override val item: ItemStack = createItem(Material.KELP) {
        name = "<white><b>Purifying Leaf"
        description = """
            
              The leaf removes all negative potion effects and gives you
              regeneration <blue>${REGEN_STRENGTH + 1}<reset> for <gray>${REGEN_DURATION}s<reset>
              Cooldown <gray>${LEAF_USE_COOLDOWN}s
              
              <green>[Right] <white>- click to use
        """
        enchant(Enchantment.UNBREAKING, 10)
        persistentDataContainer.set(UNPLACEABLE_KEY, PersistentDataType.BOOLEAN, true)
    }

    override val type: CustomItemType = CustomItemType.PURIFYING_LEAF

    private val leafCooldowns = CooldownUtil(LEAF_USE_COOLDOWN * 1000L)

    private val regenEffect = PotionEffect(PotionEffectType.REGENERATION, REGEN_DURATION * 20, REGEN_STRENGTH)

    private val negativeEffects = arrayOf(
        PotionEffectType.POISON,
        PotionEffectType.WEAKNESS,
        PotionEffectType.SLOWNESS,
        PotionEffectType.WITHER,
        PotionEffectType.NAUSEA,
        PotionEffectType.BLINDNESS,
        PotionEffectType.MINING_FATIGUE,
        PotionEffectType.DARKNESS,
    )

    private fun purify(player: Player, leafItem: ItemStack) = player.withCooldown(leafCooldowns) {
        player.removeItem(leafItem, 1)

        for (effect in negativeEffects) {
            player.removePotionEffect(effect)
        }

        addPotionEffect(regenEffect)
        playSound(location, Sound.ENTITY_PANDA_EAT, 1f, 1f)
    }

    @EventHandler
    fun onLeafUse(event: PlayerInteractEvent) {
        val item = event.rightClickItem ?: return
        if (!isCustomItem(item)) return

        purify(event.player, item)
        event.isCancelled = true
    }
}