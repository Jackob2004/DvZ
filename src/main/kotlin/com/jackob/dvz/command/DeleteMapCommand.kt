package com.jackob.dvz.command

import com.jackob.dvz.setup.FormType
import com.jackob.dvz.setup.MapFormsManager
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

class DeleteMapCommand : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<out String>) {
        val player = source.sender as? Player ?: return
        MapFormsManager.getForm(FormType.DELETE_MAP_FORM).openForm(player)
    }

    override fun permission(): String? {
        return "dvz.map-delete"
    }
}