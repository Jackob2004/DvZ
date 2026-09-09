package com.jackob.dvz.command

import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.util.mm
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class ShowPlayerStatsCommand: BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        val player = source.sender as? Player ?: return

        val playerName = if (args.size == 1) args[0] else player.name
        val searchedPlayer = Bukkit.getOfflinePlayerIfCached(playerName)?.uniqueId

        if (searchedPlayer == null) {
            player.sendMessage("<gray>This player does not exist.".mm())
        } else {
            LobbyStateHandler.showStatsMenu(player, searchedPlayer)
        }
    }

    override fun suggest(commandSourceStack: CommandSourceStack, args: Array<out String>): Collection<String> {
        return Bukkit.getOnlinePlayers().map { it.name }
    }
}
