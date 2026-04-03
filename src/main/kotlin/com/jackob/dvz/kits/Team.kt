package com.jackob.dvz.kits

import net.kyori.adventure.text.format.NamedTextColor

enum class Team(val teamName: String, val color: NamedTextColor) {
    DWARF("Dwarf", NamedTextColor.GREEN),
    ZOMBIE("Zombie", NamedTextColor.DARK_RED)
}