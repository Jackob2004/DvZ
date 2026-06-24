package com.jackob.dvz.core.objects

import com.jackob.dvz.core.GameManager
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.sync
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.managers.RegionManager
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask

class ShrineManager(private val numberOfShrines: Int, private val onlinePlayers: Collection<Player>, world: World) {

    private val regionManager: RegionManager =
        WorldGuard.getInstance().platform.regionContainer.get(BukkitAdapter.adapt(world))!!

    private val shrines: Array<Shrine> = Array(numberOfShrines) { idx ->
        Shrine(10, 20, 1 + idx, numberOfShrines + idx, idx, regionManager)
    }

    private var currentShrine = 0

    private var updateTask: BukkitTask? = null

    private val shrineInfoBar = BossBar.bossBar(
        generateInfoBarName().mm(),
        0.0F,
        BossBar.Color.YELLOW,
        BossBar.Overlay.PROGRESS
    )

    private fun generateInfoBarName(): String = "Shrine (${currentShrine.plus(1)}/$numberOfShrines)"

    private fun updateInfoBar() {
        val shrineData = shrines[currentShrine].getShrineData()
        val barProgressLevel = (shrineData.health * 100.0F / shrineData.maxHealth / 100.0F).coerceIn(0.0F, 1.0F)
        val barColor = if (shrineData.shielded) BossBar.Color.YELLOW else BossBar.Color.BLUE

        shrineInfoBar.name(generateInfoBarName().mm())
        shrineInfoBar.progress(barProgressLevel)
        shrineInfoBar.color(barColor)
    }

    fun startShrineTicking() {
        check(updateTask == null) { "Shrine ticking already running!!!" }
        onlinePlayers.forEach(::addViewer)

        updateTask = sync(delay = TimeUnit.TICKS(1), period = TimeUnit.SECONDS(1)) {
            val filteredPlayers = onlinePlayers.filter { GameManager.getPlayerTeam(it) != null }
            for (shrine in shrines) {
                shrine.onUpdate(filteredPlayers)
            }
            updateInfoBar()
        }
    }

    fun stopShrineTicking() {
        if (updateTask != null && !updateTask!!.isCancelled) {
            updateTask!!.cancel()
            onlinePlayers.forEach(::removeViewer)
            updateTask = null
        }
    }

    /**
     * @return false if there is no more shrines to activate
     */
    fun activateNextShrine(): Boolean {
        if (currentShrine + 1 >= shrines.size) return false

        currentShrine++
        shrines[currentShrine].activateShrine()
        return true
    }

    /**
     * Removes player from viewers of shine info bar
     */
    fun removeViewer(player: Player) {
        shrineInfoBar.removeViewer(player)
    }

    /**
     * Adds player to viewers of shine info bar
     */
    fun addViewer(player: Player) {
        shrineInfoBar.addViewer(player)
    }

}