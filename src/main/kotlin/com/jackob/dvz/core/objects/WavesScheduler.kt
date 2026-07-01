package com.jackob.dvz.core.objects

import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.sync
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

/**
 * Schedules waves of special monsters according to number of alive dwarves
 */
class WavesScheduler(private val dwarfTeam: Team) {

    private val specialZombieKits: List<KitType> =
        KitType.entries.filter { it.team == TeamType.ZOMBIE && it.isHero }

    private var schedulerTask: BukkitTask? = null

    private var playsInCurrWave: Int = 0

    private var lastKitIdx: Int? = null

    var currentKit: ItemStack? = null

    private fun prepareWaveKit() {
        var randomIdx = Random.nextInt(specialZombieKits.size)

        if (lastKitIdx != null) {
            while (lastKitIdx == randomIdx) {
                randomIdx = Random.nextInt(specialZombieKits.size)
            }
        }

        currentKit = specialZombieKits[randomIdx].toItem()
        lastKitIdx = randomIdx
    }

    private fun calculateNumberOfPlays() {
        val waveSize = specialZombieKits[lastKitIdx!!].waveSize!!
        check(waveSize in 0.0..1.0) { "waveSize must be between 0.0 and 1.0, but was $waveSize" }

        val aliveDwarves = dwarfTeam.getOnlineCount()
        val plays = (aliveDwarves * waveSize).toInt()

        playsInCurrWave = if (plays == 0) 1 else plays
    }

    fun anyPlaysAvailable(): Boolean {
        if (playsInCurrWave == 0) return false

        playsInCurrWave--
        if (playsInCurrWave == 0) {
            currentKit = null
        }

        return true
    }

    fun startScheduler() {
        check(schedulerTask == null) { "Scheduler task already started!!!" }

        val waveInterval = ConfigStorage.ZOMBIE_WAVE_INTERVAL.toLong()
        schedulerTask = sync(delay = TimeUnit.SECONDS(waveInterval), period = TimeUnit.SECONDS(waveInterval)) {
            prepareWaveKit()
            calculateNumberOfPlays()
            Bukkit.broadcast("<dark_red>Wave of special monsters have arrived!!".mm())
            Bukkit.getOnlinePlayers().forEach { it.playSound(it.location, Sound.BLOCK_SNIFFER_EGG_CRACK, 1.0f, 1.0f) }
        }
    }

    fun stopScheduler() {
        schedulerTask?.cancel()
        schedulerTask = null
    }
}