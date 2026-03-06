package com.jackob.dvz.setup

import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import io.papermc.paper.connection.PlayerGameConnection
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.event.player.PlayerCustomClickEvent
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

private const val MAP_TO_DELETE_KEY = "map_name"

@Suppress("UnstableApiUsage")
class MapDeleteForm : CustomForm {
    override val type: FormType
        get() = FormType.DELETE_MAP_FORM

    override fun openForm(player: Player) {
        val mapNames = MapStorage.getMapKeys()
        if (mapNames == null) {
            player.sendMessage("<gray>There are no maps configured!".withPrefix().mm())
            return
        }

        val entries = mapNames.map { SingleOptionDialogInput.OptionEntry.create(it, it.mm(), false) }

        val form = Dialog.create { builder ->
            builder
                .empty()
                .base(
                    DialogBase.builder("Map deletion".mm())
                        .body(
                            listOf(
                                DialogBody.plainMessage("<gray>Select map you want to delete".mm())
                            )
                        )
                        .inputs(
                            listOf(
                                DialogInput.singleOption(MAP_TO_DELETE_KEY, "Actual map folder name".mm(), entries)
                                    .width(250)
                                    .build()
                            )
                        )
                        .canCloseWithEscape(false)
                        .build()
                )
                .type(
                    DialogType.confirmation(
                        ActionButton.builder("<green>Confirm".mm())
                            .action(DialogAction.customClick(Key.key(type.key + "yes"), null))
                            .build(),
                        ActionButton.builder("<yellow>Cancel".mm())
                            .action(DialogAction.customClick(Key.key(type.key + "no"), null))
                            .build()
                    )
                )
        }

        player.showDialog(form)
    }

    override fun handleClick(event: PlayerCustomClickEvent, answer: String) {
        if (answer != "yes") return

        val view = event.dialogResponseView

        view!!.getText(MAP_TO_DELETE_KEY)?.let {
            val player = (event.commonConnection as PlayerGameConnection).player
            if (MapStorage.deleteMap(it)) {
                player.sendMessage("<green>Successfully deleted map!".withPrefix().mm())
            } else {
                player.sendMessage("<red>Couldn't delete the map!".withPrefix().mm())
            }
        }
    }
}