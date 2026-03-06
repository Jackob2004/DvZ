package com.jackob.dvz.setup

import io.papermc.paper.event.player.PlayerCustomClickEvent
import org.bukkit.entity.Player

interface CustomForm {
    val type: FormType
    fun openForm(player: Player)

    @Suppress("UnstableApiUsage")
    fun handleClick(event: PlayerCustomClickEvent, answer: String)
}