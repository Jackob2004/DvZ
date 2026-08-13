package com.jackob.dvz.core.equipment

import com.jackob.dvz.util.*
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

private const val MAX_MANA = 1000
private const val MANA_REGEN_RATE = 15
private const val MANA_COST = 250

class HealingAle : CustomItem(), Listener {

    override val item: ItemStack = createItem(Material.DRAGON_BREATH) {
        name = "<b><color:#FF1493>Healing Ale"
        description = """
            
            <green>[Right]<white> - click to heal yourself fully.
            Single usage eats up $MANA_COST mana out of $MAX_MANA mana bank.
            Mana regenerates by $MANA_REGEN_RATE every second.
        """
        enchant(Enchantment.UNBREAKING, 10)
        persistentDataContainer.set(ManaUtil.MANA_ITEM, PersistentDataType.BOOLEAN, true)
    }

    override val type: CustomItemType = CustomItemType.HEALING_ALE

    private val manaMap = Object2IntLinkedOpenHashMap<UUID>()

    companion object {
        private val healEffect = PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 2, false, false)
    }

    init {
        sync(period = TimeUnit.SECONDS(1)) {
            for (id in manaMap.keys) {
                val player = id.toPlayer() ?: continue
                if (!player.isOnline) continue
                addMana(id)

                if (isCustomItem(player.inventory.itemInMainHand)) {
                    displayManaStatus(id, player)
                }
            }
        }
    }

    override fun onReceive(player: Player) {
        manaMap.put(player.uniqueId, MAX_MANA)
    }

    override fun onLose(player: Player) {
        manaMap.removeInt(player.uniqueId)
    }

    private fun addMana(id: UUID) {
        val currMana = manaMap.getInt(id)
        val updatedMana = (currMana + MANA_REGEN_RATE).coerceIn(0, MAX_MANA)

        manaMap[id] = updatedMana
    }

    private fun consumeMana(id: UUID): Boolean {
        val currMana = manaMap.getInt(id)
        if (currMana - MANA_COST < 0) return false
        val updatedMana = (currMana - MANA_COST).coerceIn(0, MAX_MANA)

        manaMap[id] = updatedMana

        return true
    }

    private fun displayManaStatus(id: UUID, player: Player) {
        val currMana = manaMap.getInt(id)
        val manaDisplayVal = ((currMana * 100 / MAX_MANA) / 100.0).toFloat()
        player.exp = manaDisplayVal
    }

    @EventHandler
    fun onAleClick(e: PlayerInteractEvent) {
        val item = e.rightClickItem ?: return
        if (!isCustomItem(item)) return

        val player = e.player
        val id = player.uniqueId
        if (!manaMap.containsKey(id)) return

        if (consumeMana(id)) {
            displayManaStatus(id, player)
            player.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1f, 1f)
            player.addPotionEffect(healEffect)
        }
    }

    @EventHandler
    fun onSlotChange(e: PlayerItemHeldEvent) {
        val player = e.player
        val id = player.uniqueId
        if (!manaMap.containsKey(id)) return

        val item = player.inventory.getItem(e.newSlot)
        if (item != null && isCustomItem(item)) {
            displayManaStatus(id, player)
        } else {
            player.exp = 0f
        }
    }

}