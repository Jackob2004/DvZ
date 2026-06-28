package com.jackob.dvz.util

import org.bukkit.Location
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
