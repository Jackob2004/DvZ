package com.jackob.dvz.util

import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block

fun Location.getSphere(radius: Int, hollow: Boolean): MutableList<Location> {
    val circleBlocks = mutableListOf<Location>()

    val bx = x.toInt()
    val by = y.toInt()
    val bz = z.toInt()

    for (x in bx - radius..bx + radius) {
        for (y in by - radius..by + radius) {
            for (z in bz - radius..bz + radius) {
                val distance = ((bx - x) * (bx - x) + ((bz - z) * (bz - z)) + ((by - y) * (by - y)))

                if (distance < radius * radius && !(hollow && distance < ((radius - 1) * (radius - 1)))) {
                    val l = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                    circleBlocks.add(l)
                }
            }
        }
    }

    return circleBlocks
}

/**
 * Packs X, Y, and Z into a single 64-bit long.
 * X: 26 bits | Z: 26 bits | Y: 12 bits
 */
fun Block.packCoordinates(): Long {
    return ((x and 0x3FFFFFF).toLong() shl 38) or
            ((z and 0x3FFFFFF).toLong() shl 12) or
            ((y and 0xFFF).toLong())
}

/**
 * Packs X, Y, and Z into a single 64-bit long.
 * X: 26 bits | Z: 26 bits | Y: 12 bits
 */
fun Location.packCoordinates(): Long {
    return ((x.toLong() and 0x3FFFFFF) shl 38) or
            ((z.toLong() and 0x3FFFFFF) shl 12) or
            ((y.toLong() and 0xFFF))
}

/**
 * Expects bits to be packed in the following manner:
 * X: 26 bits | Z: 26 bits | Y: 12 bits
 */
fun Long.unpackToLocation(world: World): Location {
    val x = (this shr 38).toInt()

    val z = (((this shr 12).toInt() and 0x3FFFFFF) shl 6) shr 6
    val y = ((this and 0xFFF).toInt() shl 20) shr 20

    return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
}

fun getRegionCenterLocation(world: World, region: ProtectedRegion): Location {
    val min = region.minimumPoint
    val max = region.maximumPoint

    val centerX = (min.x() + max.x()) / 2.0
    val centerY = (min.y() + max.y()) / 2.0
    val centerZ = (min.z() + max.z()) / 2.0

    return Location(world, centerX, centerY, centerZ)
}
