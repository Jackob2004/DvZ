package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.events.LightSourceBreakEvent
import com.jackob.dvz.core.events.ShrineDamageEvent
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.sync
import com.jackob.dvz.util.toPlayer
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*

private const val MAX_MANA = 65535

class ManaManger : Listener {

    /**
     * value(upper bits - bonus, lower bits - amount)
     */
    private val playerManaVaults: Object2IntLinkedOpenHashMap<UUID> = Object2IntLinkedOpenHashMap()

    private var updateTask: BukkitTask? = null

    private fun pack(bonus: Int, amount: Int): Int {
        require(amount in 0..MAX_MANA) { "Amount must be between 0 and 65535." }
        require(bonus in 0..MAX_MANA) { "Bonus must be between 0 and 65535." }

        return (bonus shl 16) or amount
    }

    private fun getBonus(data: Int): Int {
        return data ushr 16
    }

    private fun getAmount(data: Int): Int {
        return data and 0xFFFF
    }

    /**
     * Passively adds mana to active zombie players and updates the ui level
     */
    private fun startUpdateTask(): BukkitTask {
        check(updateTask == null) { "Update task is already running" }

        return sync(period = TimeUnit.SECONDS(1)) {
            for (entry in playerManaVaults.object2IntEntrySet()) {
                val player = entry.key.toPlayer() ?: continue
                if (!KitsManager.hasKit(player)) continue

                val data: Int = entry.intValue
                val bonus = getBonus(data)
                val currentAmount = getAmount(data)

                val newAmount = (currentAmount + 1 + bonus).coerceAtMost(MAX_MANA)

                entry.setValue(pack(bonus, newAmount))

                player.level = newAmount
            }
        }
    }

    private fun addMana(player: Player, value: Int) {
        val id = player.uniqueId
        if (!playerManaVaults.containsKey(id)) return

        val data: Int = playerManaVaults.getInt(id)
        val bonus = getBonus(data)
        val currentAmount = getAmount(data)

        val newAmount = (currentAmount + value).coerceAtMost(MAX_MANA)

        playerManaVaults.put(id,pack(bonus, newAmount))
    }

    fun consumeMana(player: Player, cost: Int): Boolean {
        val uuid = player.uniqueId

        if (!playerManaVaults.containsKey(uuid)) return false

        val data: Int = playerManaVaults.getInt(uuid)
        val amount = getAmount(data)
        val bonus = getBonus(data)

        if (amount >= cost) {
            playerManaVaults.put(uuid, pack(bonus, amount - cost))
            return true
        }

        return false
    }

    fun getMana(player: Player): Int? {
        if (!playerManaVaults.containsKey(player.uniqueId)) return null

        return getAmount(playerManaVaults.getInt(player.uniqueId))
    }

    fun addPlayer(player: Player, bonus: Int = 0) {
        playerManaVaults.putIfAbsent(player.uniqueId, pack(bonus, 0))
    }

    fun register() {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
        updateTask = startUpdateTask()
    }

    fun unregister() {
        HandlerList.unregisterAll(this)
        updateTask?.cancel()
        updateTask = null
        playerManaVaults.clear()
    }

    @EventHandler
    fun onPlayerExpChange(e: PlayerExpChangeEvent) {
        e.amount = 0
    }

    @EventHandler
    fun onLightSourceBreak(e: LightSourceBreakEvent) {
        addMana(e.zombie, 8)
    }

    @EventHandler
    fun onShrineDamage(e: ShrineDamageEvent) {
        for (p in e.participants) {
            addMana(p, 2)
        }
    }

}