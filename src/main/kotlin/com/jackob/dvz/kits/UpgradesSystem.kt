package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.math.max

@DslMarker
annotation class UpgradesDsl

data class UpgradeBranch<T>(
    val id: Int,
    val type: UpgradeType,
    val cost: Int,
    val levels: List<UpgradeLevel<T>>,
    val actions: List<(Player, T) -> Unit>,
    val blockingUpgrades: List<Int>? = null,
    val icon: ItemStack
) {
    fun applyUpgrade(player: Player, levelIndex: Int, actionIndex: Int = 0) {
        if (levels.isEmpty() || actions.isEmpty()) return

        val stats = levels[levelIndex].stats
        actions[actionIndex](player, stats)
    }
}

data class UpgradeLevel<T>(
    val stats: T
)

data class UpgradeData(
    val upgradeLevel: Int,
    val manaCost: Int,
    val icon: ItemStack
)

/**
 * @param MODIFIER either item, attribute or potion effect
 */
enum class UpgradeType {
    ACTIVE_ABILITY,
    PASSIVE_ABILITY,
    MODIFIER
}

class UpgradesManager(
    private val upgrades: Array<UpgradeBranch<*>?>,
    private val tiers: Array<Int>
) {
    private val playerUpgrades: Object2LongOpenHashMap<UUID> = Object2LongOpenHashMap()

    /**
     * @return upgrade level index
     */
    private fun findMaxUnlockedUpgrade(upgrade: UpgradeBranch<*>, idx: Int, allUpgrades: Long): Int {
        val maxIdx = idx + upgrade.levels.size - 1
        var maxUnlockedUpgrade = idx
        while (maxUnlockedUpgrade < maxIdx) {
            if (!allUpgrades.hasUpgrade(maxUnlockedUpgrade + 1)) break
            maxUnlockedUpgrade += 1
        }

        return maxUnlockedUpgrade - idx
    }

    /**
     * @return start, end - inclusive, null if all tiers are already unlocked
     */
    private fun getTierBounds(allUpgrades: Long): Pair<Int, Int>? {
        val unlockedUpgrades = allUpgrades.countOneBits()
        DvZ.INSTANCE.logger.info { "unlocked: $unlockedUpgrades, max: ${upgrades.size}, tierI: ${tiers[0]}" }
        if (unlockedUpgrades == upgrades.size) return null

        var tierIdx = 0

        while (tierIdx < tiers.size) {
            if (unlockedUpgrades < tiers[tierIdx]) break
            tierIdx++
        }

        val start = if (tierIdx == 0) 0 else tiers[tierIdx - 1]
        val end = tiers[tierIdx] - 1

        return Pair(start, end)
    }

    fun hasUpgrade(player: Player, index: Int): Boolean {
        if (!playerUpgrades.containsKey(player.uniqueId)) return false
        return playerUpgrades.getLong(player.uniqueId).hasUpgrade(index)
    }

    fun hasUpgrade(player: Player, index: Enum<*>): Boolean {
        return hasUpgrade(player, index.ordinal)
    }

    fun unlockUpgrade(player: Player, index: Int) {
        if (!playerUpgrades.containsKey(player.uniqueId)) return
        playerUpgrades.put(player.uniqueId, playerUpgrades.getLong(player.uniqueId).unlockUpgrade(index))
    }

    fun getApplicableUpgradesData(player: Player): List<UpgradeData>? {
        if (!playerUpgrades.containsKey(player.uniqueId)) return null

        val playerUpgradeFlags: Long = playerUpgrades.getLong(player.uniqueId)
        val tierBound = getTierBounds(playerUpgradeFlags) ?: return null
        val icons = mutableListOf<UpgradeData>()

        for (idx in tierBound.first..tierBound.second) {
            val upgrade = upgrades[idx] ?: continue
            val hasBlockingUpgrades = upgrade.blockingUpgrades?.any { playerUpgradeFlags.hasUpgrade(it) } ?: false
            if (hasBlockingUpgrades) continue

            val level = if (!playerUpgradeFlags.hasUpgrade(idx)) {
                1
            } else findMaxUnlockedUpgrade(upgrade, idx, playerUpgradeFlags) + 2

            val manaCost = upgrade.cost * level
            if (level == upgrade.levels.size + 1) continue

            val icon = upgrade.icon.clone()
            icon.editPersistentDataContainer {
                it.set(UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, level)
                it.set(UPGRADE_COST_KEY, PersistentDataType.INTEGER, manaCost)
            }
            icons.add(UpgradeData(level, manaCost, icon))
        }

        return icons.ifEmpty { null }
    }

    /**
     * Applies all upgrades with UpgradeType.Modifier
     */
    fun applyModifiers(player: Player) {
        val id = player.uniqueId
        if (!playerUpgrades.containsKey(id)) return

        val playerUpgradeFlags: Long = playerUpgrades.getLong(id)

        for ((idx, value) in upgrades.withIndex()) {
            if (value == null) continue
            if (value.type != UpgradeType.MODIFIER) continue
            if (!playerUpgradeFlags.hasUpgrade(idx)) continue

            val level = findMaxUnlockedUpgrade(value, idx, playerUpgradeFlags)
            value.applyUpgrade(player, level)
        }
    }

    /**
     * Applies upgrade that is not UpgradeType.Modifier
     * Assumes the player has the passed upgrade
     */
    fun applyAbility(player: Player, upgradeId: Enum<*>, fnIdx: Int) {
        val id = player.uniqueId
        if (!playerUpgrades.containsKey(id)) return

        val playerUpgradeFlags: Long = playerUpgrades.getLong(id)
        val idx = upgradeId.ordinal

        val upgrade = upgrades[idx] ?: return
        if (upgrade.type == UpgradeType.MODIFIER) return

        val level = findMaxUnlockedUpgrade(upgrade, idx, playerUpgradeFlags)

        upgrade.applyUpgrade(player, level, fnIdx)
    }


    fun addPlayer(player: Player) {
        playerUpgrades.putIfAbsent(player.uniqueId, 0L)
    }

    companion object {
        fun create(numberOfUpgrades: Int, numberOfTiers: Int, init: UpgradesBuilder.() -> Unit): UpgradesManager {
            val builder = UpgradesBuilder(numberOfUpgrades, numberOfTiers)
            builder.init()
            return builder.build()
        }


        val UPGRADE_KEY = NamespacedKey(DvZ.INSTANCE, "upgrade-key")
        val UPGRADE_LEVEL_KEY = NamespacedKey(DvZ.INSTANCE, "upgrade-level-key")
        val UPGRADE_COST_KEY = NamespacedKey(DvZ.INSTANCE, "upgrade-cost-key")
    }
}

@UpgradesDsl
class UpgradesBuilder(numberOfUpgrades: Int, numberOfTiers: Int) {
    private val upgrades: Array<UpgradeBranch<*>?> = Array(numberOfUpgrades) { null }
    private val tiers: Array<Int> = Array(numberOfTiers) { 0 }

    fun tier(index: Int, init: TierBuilder.() -> Unit) {
        val builder = TierBuilder(index, upgrades, tiers)
        builder.init()
    }

    fun build(): UpgradesManager {
        return UpgradesManager(upgrades, tiers)
    }
}

@UpgradesDsl
class TierBuilder(
    private val tierIndex: Int,
    private val upgrades: Array<UpgradeBranch<*>?>,
    private val tiers: Array<Int>,
) {
    fun <T> upgrade(
        id: Int,
        type: UpgradeType,
        cost: Int,
        blockingUpgrades: List<Int>?,
        init: UpgradeBuilder<T>.() -> Unit
    ) {
        val builder = UpgradeBuilder<T>(id, type, cost, blockingUpgrades)
        builder.init()
        val branch = builder.build()

        upgrades[id] = branch
        tiers[tierIndex] = max(tiers[tierIndex], id + branch.levels.size)
    }

    fun <T> upgrade(
        id: Enum<*>,
        type: UpgradeType,
        cost: Int,
        blockingUpgrades: List<Enum<*>>? = null,
        init: UpgradeBuilder<T>.() -> Unit
    ) {
        upgrade(id.ordinal, type, cost, blockingUpgrades?.map { it.ordinal }, init)
    }
}

@UpgradesDsl
class UpgradeBuilder<T>(
    private val id: Int,
    private val type: UpgradeType,
    private val cost: Int,
    private val blockingUpgrades: List<Int>? = null
) {
    private val levels = ArrayList<UpgradeLevel<T>>(5)
    private val actions = ArrayList<(Player, T) -> Unit>(3)
    var icon: ItemStack = ItemStack(Material.DIRT)

    fun action(block: (Player, T) -> Unit) {
        actions.add(block)
    }

    fun level(stats: T) {
        levels.add(UpgradeLevel(stats))
    }

    fun build(): UpgradeBranch<T> {
        icon.editPersistentDataContainer { container ->
            container.set(UpgradesManager.UPGRADE_KEY, PersistentDataType.INTEGER, id)
        }
        return UpgradeBranch(id, type, cost, levels.toList(), actions.toList(), blockingUpgrades, icon)
    }
}

fun Long.hasUpgrade(index: Int): Boolean {
    return (this and (1L shl index)) != 0L
}

fun Long.unlockUpgrade(index: Int): Long {
    require(index in 0..63) { "Invalid upgrade index: $index" }
    return this or (1L shl index)
}