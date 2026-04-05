package com.jackob.dvz.util

import com.jackob.dvz.DvZ
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

fun sync(delay: Long = 0, period: Long, init: BukkitRunnable.() -> Unit): BukkitTask {
    return object : BukkitRunnable() {
        override fun run() {
            init()
        }

    }.runTaskTimer(DvZ.INSTANCE, delay, period)
}

fun sync(init: BukkitRunnable.() -> Unit): BukkitTask {
    return object : BukkitRunnable() {
        override fun run() {
            init()
        }

    }.runTask(DvZ.INSTANCE)
}

fun async(delay: Long = 0, period: Long, init: BukkitRunnable.() -> Unit): BukkitTask {
    return object : BukkitRunnable() {
        override fun run() {
            init()
        }

    }.runTaskTimerAsynchronously(DvZ.INSTANCE, delay, period)
}