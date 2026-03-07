package com.jackob.dvz.storage

import org.bukkit.Location

data class GameMap(
    var name: String,
    var dwarfSpawn: Location,
    var zombieSpawn: Location,
    var goldmine: Location,
    var sawmill: Location,
    var oil: Location,
    val shrines: List<Location>
)
