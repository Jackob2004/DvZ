package com.jackob.dvz.command

import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

/**
 * Sets one of the possible lobby locations and saves to the yml file
 * @see MapStorage.LobbyLocation
 */
class SetLobbyLocCommand : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        val player = source.sender as? Player ?: return

        if (args.isEmpty()) {
            player.sendMessage("<yellow>You need to provide location name".mm())
            return
        }

        val name = args[0]
        try {
            val test = MapStorage.LobbyLocation.valueOf(name)
            if (test.saveLobbyLocation(player.location)) {
                player.sendMessage("<green>${test.locationName} location saved successfully!".withPrefix().mm())
                player.sendMessage("<gray>Effect will take effect after server restart...".withPrefix().mm())
            } else {
                player.sendMessage("<red>Saving lobby location failed!".withPrefix().mm())
            }
        } catch (_: Exception) {
            player.sendMessage("<yellow>Unknown name".mm())
        }
    }

    override fun suggest(commandSourceStack: CommandSourceStack, args: Array<out String>): Collection<String> {
        return MapStorage.LobbyLocation.entries.map { it.name }
    }

    override fun permission(): String? {
        return "dvz.save-lobby"
    }
}