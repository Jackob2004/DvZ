package com.jackob.dvz.core.objects

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.KitType
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.util.TimeUnit
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.sync
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

class HeroPricker(players: Collection<Player>) : Listener {

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    private val heroesNumber = players.size / ConfigStorage.PLAYERS_PER_HERO

    private val selectionItem = createItem(Material.NETHER_STAR) {
        name = "<rainbow><b>Click to play as a hero"
    }

    private val playerPools = players.chunked(heroesNumber)

    private val selectedPlayers: MutableSet<Player> = HashSet(heroesNumber)

    private val selectionTasks: Array<BukkitTask> = Array(heroesNumber) { index ->
        startSelectionTask(index)
    }

    private fun selectPlayer(player: Player) {
        player.inventory.remove(selectionItem)
        selectedPlayers.add(player)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
    }

    private fun giveSelectionItem(player: Player) {
        player.inventory.addItem(selectionItem)
        player.playSound(player.location, Sound.ENTITY_VILLAGER_TRADE, 1f, 1f)

        player.sendMessage("<gold><b>You've been chosen by gods to ascend as a hero!!!".mm())
        player.sendMessage("<aqua><u>Click Nether Star in your inventory to accept your destiny".mm())
    }

    private fun startSelectionTask(poolIndex: Int): BukkitTask {
        val pool = playerPools[poolIndex].shuffled().toMutableSet()
        val waitTime = TimeUnit.SECONDS(ConfigStorage.HERO_SELECT_TIME.toLong())

        var candidate = pool.first()
        giveSelectionItem(candidate)
        return sync(delay = waitTime, period = waitTime) {
            pool.remove(candidate)
            candidate.inventory.remove(selectionItem)

            if (selectedPlayers.contains(candidate) || pool.isEmpty()) {
                cancel()
                return@sync
            }

            candidate = pool.first()
            giveSelectionItem(candidate)
        }
    }

    private fun assignRandomHeroes(): Collection<Pair<Player, KitType>> {
        val heroes = ArrayList<Pair<Player, KitType>>(heroesNumber)
        val availableTypes = KitType.entries.filter { it.isHero }

        if (availableTypes.size < heroesNumber) {
            selectedPlayers.forEach {
                heroes.add(Pair(it, availableTypes[Random.nextInt(availableTypes.size)]))
            }
        } else {
            val shuffledTypes = availableTypes.shuffled()
            for ((idx, player) in selectedPlayers.withIndex()) {
                heroes.add(Pair(player, shuffledTypes[idx]))
            }
        }

        selectedPlayers.clear()
        return heroes
    }

    private fun stopSelectionTasks() {
        selectionTasks.forEach {
            if (!it.isCancelled) {
                it.cancel()
            }
        }
    }

    private fun startLastResortSelection() {
        val onlinePlayers = Bukkit.getOnlinePlayers().filter { !selectedPlayers.contains(it) }.toMutableList()
        while (selectedPlayers.size != heroesNumber && !onlinePlayers.isEmpty()) {
            selectedPlayers.add(onlinePlayers.removeLast())
        }
    }

    fun retrieveHeroes(): Collection<Pair<Player, KitType>> {
        HandlerList.unregisterAll(this)
        stopSelectionTasks()
        startLastResortSelection()

        return assignRandomHeroes()
    }

    @EventHandler
    fun onItemClick(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return

        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type != Material.NETHER_STAR) return

        selectPlayer(player)
    }

    companion object {
        fun canPickAnyHero(playersCount: Int): Boolean {
            return playersCount / ConfigStorage.PLAYERS_PER_HERO != 0
        }
    }
}