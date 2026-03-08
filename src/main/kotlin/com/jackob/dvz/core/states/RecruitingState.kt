package com.jackob.dvz.core.states

import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.storage.MapStorage
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.name
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent

class RecruitingState(var gameMap: GameMap) : GameState {

    var wasMapRerolled = false

    override fun onEnter() {
        super.onEnter()
    }

    override fun onLeave() {
        super.onLeave()
    }

    private fun giveLobbyTools(player: Player) {
        val teleportTool = createItem(Material.COMPASS) {
            name = "<white><b>Teleport options"
            description = """
                <gray>Use it to switch between lobby and the current map.
                <gray>Get to know the map better by exploring its key locations.
            """
        }

        player.inventory.addItem(teleportTool)
    }

    private fun openTeleportOptions(player: Player) {
        TODO("Implement this")
    }

    fun performMapReroll(rerolledMap: GameMap) {
        if (wasMapRerolled) return
        wasMapRerolled = true

        gameMap = rerolledMap
        Bukkit.broadcast("<gray><i>Map Rerolled!!!".withPrefix().mm())
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        player.teleport(MapStorage.LOBBY_SPAWN!!)
        player.resetAll()
        giveLobbyTools(player)
    }

    @EventHandler
    fun onLobbyToolClick(event: PlayerInteractEvent) {
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onLobbyInteract(event: PlayerInteractEvent) {
        if (!event.player.hasPermission("dvz.lobby.interact")) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        if (!event.player.hasPermission("dvz.lobby.interact")) {
            event.isCancelled = true
        }
    }

}