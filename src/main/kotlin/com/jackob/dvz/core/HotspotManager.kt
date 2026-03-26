package com.jackob.dvz.core

import com.jackob.dvz.DvZ
import org.bukkit.Chunk
import org.bukkit.Location

object HotspotManager {

    private val activeHotspots: MutableSet<Chunk> = HashSet()

    fun addHotspot(vararg locations: Location) {
        locations.forEach {
            it.world.getChunkAtAsync(it).thenAccept { chunk ->
                chunk.addPluginChunkTicket(DvZ.INSTANCE)
                activeHotspots.add(chunk)
            }
        }
    }

    fun removeHotspot(vararg locations: Location) {
        locations.forEach {
            it.chunk.removePluginChunkTicket(DvZ.INSTANCE)
            activeHotspots.remove(it.chunk)
        }
        DvZ.INSTANCE.logger.info("Hotspots: ${activeHotspots.size}")
    }

    fun clearAllHotspots() {
        activeHotspots.forEach { it.removePluginChunkTicket(DvZ.INSTANCE) }
        activeHotspots.clear()
    }
}