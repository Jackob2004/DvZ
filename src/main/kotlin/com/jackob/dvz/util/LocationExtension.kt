package com.jackob.dvz.util

import org.bukkit.Location

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
