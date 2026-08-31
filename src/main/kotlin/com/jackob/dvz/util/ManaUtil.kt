package com.jackob.dvz.util

import com.jackob.dvz.DvZ
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.min

/**
 * Helper class managing mana bank of a single player.
 * The bank is associated with a single bound item.
 *
 * The unregisterManaBank method should be called once the object is no longer used.
 */
class ManaUtil(private val ownerId: UUID, private val manaRegenRate: Int, private val boundItemKey: NamespacedKey) :
    Listener {

    private var currMana = MAX_MANA

    private val regenTask: BukkitTask = sync(period = TimeUnit.SECONDS(1)) {
        val player = ownerId.toPlayer()
        if (player != null) {
            regenMana()
        }

        if (player != null && player.holdsBoundItem()) {
            updateUI(player)
        }
    }

    companion object {
        private const val MAX_MANA = 1000

        /**
         * Key indicating that the item is bound to a specific mana bank.
         * Mana items should be marked with this key for the mana ui level to be displayed correctly.
         */
        val MANA_ITEM = NamespacedKey(DvZ.INSTANCE, "dvz-mana-item")
    }

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    fun unregisterManaBank() {
        regenTask.cancel()
        HandlerList.unregisterAll(this)
    }

    fun consumeMana(amount: Int, player: Player): Boolean {
        if (currMana - amount < 0) return false

        currMana -= amount
        updateUI(player)

        return true
    }

    private fun Player.holdsBoundItem() : Boolean {
        return this.inventory.itemInMainHand.persistentDataContainer.has(boundItemKey)
    }

    private fun regenMana() {
        currMana = min(currMana + manaRegenRate, MAX_MANA)
    }

    private fun updateUI(player: Player) {
        val mana = (currMana * 100 / MAX_MANA / 100.0).toFloat()
        player.exp = mana
    }

    @EventHandler
    fun onItemSwitch(e: PlayerItemHeldEvent) {
        val player = e.player
        if (player.uniqueId != ownerId) return
        val item = player.inventory.getItem(e.newSlot) ?: return

        if (item.persistentDataContainer.has(boundItemKey)) {
            updateUI(player)
        } else if (!item.persistentDataContainer.has(MANA_ITEM)) {
            player.exp = 0F
        }
    }

}