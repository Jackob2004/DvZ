package com.jackob.dvz.storage

import org.bukkit.Location

data class GameMap(
    var name: String,
    var dwarfSpawn: Location,
    var zombieSpawn: Location,
    val shrines: List<Location>
)
