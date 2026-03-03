package com.jackob.dvz.storage

import org.bukkit.Location
import java.util.HashMap

data class GameMapDraft(
    var name: String? = null,
    var dwarfSpawn: Location? = null,
    var zombieSpawn: Location? = null,
    var totalShrines: Int? = null,
    val shrines: MutableMap<Int, Location> = HashMap(3)
)