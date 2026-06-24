package com.jackob.dvz.core.objects

import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.sync
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.scheduler.BukkitTask
import kotlin.math.abs

class TemporalShiftTask(private val world: World) {

    private var task: BukkitTask? = null

    private var isDay = true

    private fun playTimeShift(currentTime: Long, shiftedTime: Long) {
        val steps = 4
        val step = abs(currentTime - shiftedTime) / steps

        var currStep = steps
        var updatedTime = currentTime

        sync(period = TimeUnit.SECONDS(1)) {
            updatedTime += step
            if (updatedTime > 24_000) {
                updatedTime %= 24_000
            }
            world.time = updatedTime

            currStep--
            if (currStep <= 0) {
                cancel()
            }
        }

    }

    fun startTask() {
        check(task == null) { "There can be only one day temporal shift task running!" }

        val quarterOfDayLightCycle = TimeUnit.TICKS(6000)
        val day = TimeUnit.TICKS(6000)
        val night = TimeUnit.TICKS(18_000)
        task = sync(delay = quarterOfDayLightCycle, period = quarterOfDayLightCycle) {
            if (isDay) {
                playTimeShift(world.time, night)
                Bukkit.broadcast("<i><dark_purple>Temporal shift has occurred bringing darkness and fear along with it".mm())
                Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f) }
            } else {
                playTimeShift(world.time, day)
                Bukkit.broadcast("<i><gold>Temporal shift has occurred bringing light and hope along with it".mm())
            }

            isDay = !isDay
        }
    }

    fun stopTask() {
        if (task != null && !task!!.isCancelled) {
            task!!.cancel()
            task = null
        }
    }

}