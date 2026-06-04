package com.jackob.dvz.command

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class ZombieReleaseCommand : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        val player = source.sender as? Player ?: return

        if (GameManager.releaseZombies()) {
            player.sendMessage("<green> monsters released".withPrefix().mm())
        } else {
            player.sendMessage("<red> could not release zombies!".withPrefix().mm())
        }
    }

    override fun permission(): String {
        return "dvz.zombie-release"
    }
}