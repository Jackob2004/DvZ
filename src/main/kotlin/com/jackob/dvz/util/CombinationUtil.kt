package com.jackob.dvz.util

import com.jackob.dvz.DvZ
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import java.util.UUID

class CombinationUtil(
    private val ownerId: UUID,
    private val boundItem: NamespacedKey,
    val onActionFire: (Player) -> Unit
) : Listener {

    private val currCombination: Array<ClickType?> = arrayOfNulls<ClickType?>(COMBINATION_LENGTH)

    private var index: Int = 0

    private var lastClick: Long = 0

    private val actions: HashMap<Sequence, (Player) -> Unit> = HashMap(4)

    private val combinationString = StringBuilder()

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    companion object {
        private const val CLICK_INTERVAL = 1000L
        private const val COMBINATION_LENGTH = 3
    }

    fun registerAction(sequence: Sequence, action: (Player) -> Unit) {
        require(sequence.first != ClickType.LEFT) { "Combination sequence must start with - RIGHT" }
        actions[sequence] = action
    }

    fun unregisterCombinations() {
        HandlerList.unregisterAll(this)
        combinationString.clear()
        actions.clear()
    }

    fun hasActiveCombination(): Boolean {
        return index != 0
    }

    private fun exceededInterval(): Boolean {
        val now = System.currentTimeMillis()
        val oldLastClick = lastClick
        lastClick = now

        return now - oldLastClick > CLICK_INTERVAL
    }

    private fun resetCombination() {
        currCombination.fill(null)
        index = 0
    }

    private fun updateUI(player: Player) {
        var counter = 0
        for (c in currCombination) {
            if (c != null) {
                counter++
                combinationString.append("<green>${c.symbol}")
            }

            if (c != null && counter != COMBINATION_LENGTH) {
                combinationString.append("<gray>-")
            }
        }

        while (counter != COMBINATION_LENGTH) {
            counter++
            combinationString.append("<gray><u>?<reset>")

            if (counter != COMBINATION_LENGTH) {
                combinationString.append("<gray>-")
            }
        }

        player.sendActionBar(combinationString.toString().mm())
        player.playSound(player.location, Sound.BLOCK_LEVER_CLICK, 1f, 1f)
        combinationString.clear()
    }

    private fun fireAction(player: Player) {
        val sequence = Sequence(currCombination[0]!!, currCombination[1]!!, currCombination[2]!!)
        val action = actions[sequence]

        if (action != null) {
            action(player)
            onActionFire(player)
        }
    }


    private fun registerMouseClick(clickType: ClickType, player: Player) {
        if (exceededInterval()) {
            resetCombination()
        }

        if (index == 0 && clickType == ClickType.LEFT) {
            return
        }

        currCombination[index] = clickType
        index++
        updateUI(player)

        if (index == COMBINATION_LENGTH) {
            fireAction(player)
            resetCombination()
        }
    }

    @EventHandler
    fun onItemClick(e: PlayerInteractEvent) {
        val player = e.player
        if (player.uniqueId != ownerId) return
        val item = e.item ?: return
        if (!item.persistentDataContainer.has(boundItem)) return

        val action = e.action
        val clickType = if (action.isRightClick) {
            ClickType.RIGHT
        } else if (action.isLeftClick) {
            ClickType.LEFT
        } else {
            null
        }

        if (clickType != null) {
            registerMouseClick(clickType, player)
        }
    }

    @EventHandler
    fun onItemSwitch(e: PlayerItemHeldEvent) {
        val player = e.player
        if (player.uniqueId != ownerId) return
        val item = player.inventory.getItem(e.newSlot) ?: return

        if (!item.persistentDataContainer.has(boundItem)) {
            resetCombination()
        }
    }

    enum class ClickType(val symbol: Char) {
        LEFT('L'),
        RIGHT('R')
    }

    data class Sequence(val first: ClickType, val second: ClickType, val third: ClickType)

}