package com.jackob.dvz.command

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class MapSetCommand : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        if (args.size != 1) return
        if (source.sender !is Player) return

        if (!GameManager.setMap(args[0])) {
            source.sender.sendMessage("<red>Changing map failed!!!".withPrefix().mm())
        }
    }

    override fun permission(): String? {
        return "dvz.map-set"
    }

    override fun suggest(source: CommandSourceStack, args: Array<out String>): Collection<String> {
        if (args.isEmpty()) {
            return MapStorage.getMapKeys()!!
        }

        return MapStorage.getMapKeys()!!.filter { it.startsWith(args[args.size - 1]) }
    }
}