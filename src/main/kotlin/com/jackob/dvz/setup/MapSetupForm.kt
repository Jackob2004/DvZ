package com.jackob.dvz.setup

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
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player

private const val MAP_KEY = "map_name"
private const val SHRINES_KEY = "shrines"

@Suppress("UnstableApiUsage")
class MapSetupForm : CustomForm {

    override val type: FormType
        get() = FormType.SETUP_FORM

    override fun openForm(player: Player) {
        val process = MapSetupManager.getProcess(player)!!
        val mapName = process.gameMap.name ?: ""
        val shrines = process.gameMap.totalShrines ?: 1

        val form = Dialog.create { builder ->
            builder
                .empty()
                .base(
                    DialogBase.builder("Basic map setup".mm())
                        .body(
                            listOf(
                                DialogBody.plainMessage("<gray>You need to fill up this info in order to proceed further".mm())
                            )
                        )
                        .inputs(
                            listOf(
                                DialogInput.text(MAP_KEY, "Unique map name".mm())
                                    .maxLength(16)
                                    .initial(mapName)
                                    .width(250)
                                    .build(),
                                DialogInput.numberRange(SHRINES_KEY, "<green>Number of shrines (1-5)".mm(), 1F, 5F)
                                    .step(1F)
                                    .initial(shrines.toFloat())
                                    .width(200)
                                    .labelFormat("%s: %s")
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
        val player = (event.commonConnection as PlayerGameConnection).player

        if (view!!.getText(MAP_KEY)!!.isNotBlank()) {
            val process = MapSetupManager.getProcess(player)!!

            process.gameMap.name = view.getText(MAP_KEY)
            process.gameMap.totalShrines = view.getFloat(SHRINES_KEY)?.toInt()

            player.sendMessage("<green>Basic info saved".withPrefix().mm())
        } else {
            player.sendMessage("<red>Provided map name was invalid".withPrefix().mm())
        }
    }
}