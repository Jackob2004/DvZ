package com.jackob.dvz.command

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class MapRerollCommand : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        if (source.sender !is Player) return

        if (!GameManager.rerollMap()) {
            source.sender.sendMessage("<red>Couldn't reroll map!".withPrefix().mm())
        }
    }

    override fun permission(): String? {
        return "dvz.map-reroll"
    }
}