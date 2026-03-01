package com.jackob.dvz.command

import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class SaveLobbyCommand : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        val player = source.sender as? Player ?: return

        if (MapStorage.saveLobby(player.location)) {
            player.sendMessage("<green>Lobby location saved successfully!".withPrefix().mm())
        } else {
            player.sendMessage("<red>Saving lobby location failed!".withPrefix().mm())
        }
    }
}