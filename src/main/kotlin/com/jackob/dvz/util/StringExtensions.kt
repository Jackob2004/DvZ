package com.jackob.dvz.util

import com.jackob.dvz.DvZ
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.attribute.Attribute

const val PREFIX = "<gold><b>DvZ: <reset>"

fun String.mm(): Component {
    return MiniMessage.miniMessage().deserialize(this)
}

fun String.withPrefix(): String {
    return PREFIX + this
}

fun String.toAttribute(): Attribute {
    return Registry.ATTRIBUTE.get(NamespacedKey.minecraft(this.lowercase()))!!
}

fun String.toSound(): Sound? {
    val key = NamespacedKey.minecraft(this.lowercase())
    val sound = Registry.SOUND_EVENT.get(key)
    if (sound == null) DvZ.INSTANCE.logger.warning("Sound not found for key: '$key'")
    return sound
}