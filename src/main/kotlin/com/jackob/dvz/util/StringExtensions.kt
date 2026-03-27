package com.jackob.dvz.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute

const val PREFIX = "<gold><b>DvZ: <reset>"

fun String.mm(): Component {
    return MiniMessage.miniMessage().deserialize(this)
}

fun String.withPrefix(): String {
    return PREFIX + this
}

fun String.toAttribute() : Attribute {
    return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(this.lowercase()))!!
}