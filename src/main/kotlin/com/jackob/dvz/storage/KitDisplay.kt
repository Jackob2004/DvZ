package com.jackob.dvz.storage

import com.jackob.dvz.util.mm
import net.kyori.adventure.text.Component
import org.bukkit.Material

data class KitDisplay(
    val icon: Material = Material.DIRT,
    val name: Component = "Not set".mm(),
    val description: Component = "Not set".mm(),
)