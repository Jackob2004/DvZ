package com.jackob.dvz.storage

import org.bukkit.Material

data class KitDisplay(
    val icon: Material = Material.DIRT,
    val name: String = "Not set",
    val description: List<String> = ArrayList()
)