package com.jackob.dvz.util

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent

/**
 * @return false when block is bedrock or is protected by region
 */
@Suppress("UnstableApiUsage")
fun Block.isBreakable(player: Player): Boolean {
    if (this.type == Material.BEDROCK) return false

    val blockBreakSimulate = BlockBreakEvent(this, player)
    Bukkit.getPluginManager().callEvent(blockBreakSimulate)

    return !blockBreakSimulate.isCancelled
}