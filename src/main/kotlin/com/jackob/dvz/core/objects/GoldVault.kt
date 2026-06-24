package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.events.DwarfGoldCollectEvent
import com.jackob.dvz.core.events.ShrineGoldDepositEvent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.random.Random

/**
 * Manages dwarves' gold by reacting to gold-related game events.
 * * This class provides a thread-safe (async-friendly) view of the current gold total
 * and handles the logic for direct deposits versus physical inventory additions.
 */
class GoldVault : Listener {

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    private var gold: AtomicInteger = AtomicInteger()

    private val goldDepositSound: Sound = Sound.ENTITY_ITEM_PICKUP

    var canDirectlyDeposit: Boolean = true

    private fun playDepositSound(player: Player) {
        val pitch = 0.8f + Random.nextFloat() * 0.4f
        player.playSound(player.location, goldDepositSound, 1.0f, pitch)
    }

    private fun makeWithdrawal(amount: Int) : Boolean {
        if (gold.get() == 0) return false

        val updatedGold = max(gold.get() - amount, 0)
        gold.set(updatedGold)
        return true
    }

    private fun makeDeposit(player: Player, amount: Int) {
        gold.set(gold.get() + amount)
        playDepositSound(player)
    }

    fun getGoldAmount(): Int = gold.get()

    fun unregisterVault() {
        HandlerList.unregisterAll(this)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onGoldCollect(event: DwarfGoldCollectEvent) {
        if (canDirectlyDeposit) {
            makeDeposit(event.player, event.amount)
        } else {
            event.player.inventory.addItem(ItemStack(Material.GOLD_INGOT, event.amount))
        }
    }

    @EventHandler
    fun onShrineDeposit(e: ShrineGoldDepositEvent) {
        makeDeposit(e.player, e.amount)
    }

}