package com.jackob.dvz.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

const val PREFIX = "<gray><b>DvZ: <reset>"

fun String.mm(): Component {
    return MiniMessage.miniMessage().deserialize(this)
}

fun String.withPrefix(): String {
    return PREFIX + this
}