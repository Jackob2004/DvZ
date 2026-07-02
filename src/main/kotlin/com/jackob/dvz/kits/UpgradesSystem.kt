package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import com.jackob.dvz.util.description
import com.jackob.dvz.util.updateItem
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

@DslMarker
annotation class UpgradesDsl

data class UpgradeBranch<T>(
    val id: Int,
    val type: UpgradeType,
    val path: Int? = null,
    val cost: Int,
    val levels: List<UpgradeLevel<T>>,
    val actions: List<(Player, T) -> Unit>,
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

data class PathBounds(var start: Int, var end: Int)

interface BasePath {
    val pathName: String
}

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
    private val tiers: Array<Array<PathBounds>>
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

    private fun hasAllTierUpgrades(allUpgrades: Long, tierIdx: Int, pathIdx: Int): Boolean {
        val path = tiers[tierIdx][pathIdx]
        for (i in path.start..path.end) {
            if (!allUpgrades.hasUpgrade(i)) return false
        }

        return true
    }

    /**
     * @return player's path idx or null if player hasn't chosen any path yet
     */
    private fun getPlayerPath(allUpgrades: Long): Int? {
        val firstTier = tiers[0]
        for (pathIdx in 0..<firstTier.lastIndex) {
            if (allUpgrades.hasUpgrade(firstTier[pathIdx].start)) {
                return pathIdx
            }
        }

        return null
    }

    /**
     * @return last upgrade idx available in player's current tier or null if player has unlocked all upgrades
     */
    private fun calcTierBound(allUpgrades: Long, playerPath: Int?): Int? {
        if (playerPath == null) {
            return tiers[0][tiers[0].lastIndex].end
        }

        var tierIdx = 0

        while (tierIdx < tiers.size - 1) {
            if (!hasAllTierUpgrades(allUpgrades, tierIdx, playerPath)) break
            tierIdx++
        }

        return if (tierIdx == tiers.size) null else tiers[tierIdx][tiers[tierIdx].lastIndex].end
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
        val playerPath = getPlayerPath(playerUpgradeFlags)
        val tierBound = calcTierBound(playerUpgradeFlags, playerPath) ?: return null

        val icons = mutableListOf<UpgradeData>()

        for (idx in 0..tierBound) {
            val upgrade = upgrades[idx] ?: continue
            if (upgrade.path != playerPath && upgrade.path != null && playerPath != null) continue

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
        fun create(numberOfUpgrades: Int, numberOfTiers: Int, numberOfPaths: Int, init: UpgradesBuilder.() -> Unit): UpgradesManager {
            val builder = UpgradesBuilder(numberOfUpgrades, numberOfTiers, numberOfPaths)
            builder.init()
            return builder.build()
        }


        val UPGRADE_KEY = NamespacedKey(DvZ.INSTANCE, "upgrade-key")
        val UPGRADE_LEVEL_KEY = NamespacedKey(DvZ.INSTANCE, "upgrade-level-key")
        val UPGRADE_COST_KEY = NamespacedKey(DvZ.INSTANCE, "upgrade-cost-key")
    }
}

@UpgradesDsl
class UpgradesBuilder(numberOfUpgrades: Int, numberOfTiers: Int, numberOfPaths: Int) {
    private val upgrades: Array<UpgradeBranch<*>?> = Array(numberOfUpgrades) { null }
    private val tiers: Array<Array<PathBounds>> =
        Array(numberOfTiers) {
            Array(numberOfPaths + 1) { PathBounds(65,0) }
        }

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
    private val tiers: Array<Array<PathBounds>>,
) {

    private fun addPathInfo(icon: ItemStack, pathName: String?) {
        val name = pathName ?: "<gray>Neutral"
        icon.updateItem{
            val pathInfo = """
                
                <white> Path: $name
            """.trimIndent()
            description += pathInfo
        }
    }

    fun <T> upgrade(
        id: Int,
        type: UpgradeType,
        cost: Int,
        path: Int?,
        pathName: String?,
        init: UpgradeBuilder<T>.() -> Unit
    ) {
        val builder = UpgradeBuilder<T>(id, type,path, cost)
        builder.init()
        val branch = builder.build()

        val pathIdx = branch.path ?: tiers[0].lastIndex

        upgrades[id] = branch
        tiers[tierIndex][pathIdx].start = min(tiers[tierIndex][pathIdx].start, id)
        tiers[tierIndex][pathIdx].end = max(tiers[tierIndex][pathIdx].end, id + branch.levels.size - 1)
        addPathInfo(branch.icon, pathName)
    }

    fun <T, E> upgrade(
        id: Enum<*>,
        type: UpgradeType,
        cost: Int,
        path: E,
        init: UpgradeBuilder<T>.() -> Unit
    ) where E : Enum<E>, E : BasePath{
        upgrade(id.ordinal, type, cost, path.ordinal, path.pathName, init)
    }

    fun <T> upgrade(
        id: Enum<*>,
        type: UpgradeType,
        cost: Int,
        init: UpgradeBuilder<T>.() -> Unit
    ) {
        upgrade(id.ordinal, type, cost, null, null, init)
    }
}

@UpgradesDsl
class UpgradeBuilder<T>(
    private val id: Int,
    private val type: UpgradeType,
    private val path: Int?,
    private val cost: Int
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
        return UpgradeBranch(id, type, path, cost, levels.toList(), actions.toList(), icon)
    }
}

fun Long.hasUpgrade(index: Int): Boolean {
    return (this and (1L shl index)) != 0L
}

fun Long.unlockUpgrade(index: Int): Long {
    require(index in 0..63) { "Invalid upgrade index: $index" }
    return this or (1L shl index)
}