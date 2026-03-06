package com.jackob.dvz.command

import com.jackob.dvz.setup.MapSetupManager
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class SetupMapCommand : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        val player = source.sender as? Player ?: return
        MapSetupManager.openSetupMenu(player)
    }

    override fun permission(): String? {
        return "dvz.map-setup"
    }
}