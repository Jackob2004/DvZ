package com.jackob.dvz.command

import com.jackob.dvz.kits.zombie.base.Creeper
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class TestCommand: BasicCommand {
    override fun execute(
        source: CommandSourceStack,
        args: Array<out String>
    ) {
        val player = source.sender as? Player ?: return
        Creeper.CreeperListener.upgrades.resetAllUpgrades(player)
    }
}