package com.jackob.dvz.core.states

import com.jackob.dvz.core.handleLobbyDamage
import com.jackob.dvz.core.handleLobbyInteract
import com.jackob.dvz.core.handleLobbyInvClick
import com.jackob.dvz.core.handleLobbyItemDrop
import com.jackob.dvz.core.handleLobbyToolClick
import com.jackob.dvz.core.refreshToLobbyState
import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.Team
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.ui.PagerMenu
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scoreboard.Team as BukkitTeam

class PreparationState(private val gameMap: GameMap, private val selectedKits: Map<Player, KitType>) : GameState {

    private val customBoard = Bukkit.getScoreboardManager().newScoreboard

    private val dwarfTeam = customBoard.registerNewTeam(Team.DWARF.teamName).apply {
        setAllowFriendlyFire(false)
        setOption(BukkitTeam.Option.COLLISION_RULE, BukkitTeam.OptionStatus.NEVER)
        color(Team.DWARF.color)
    }

    override fun onEnter() {
        selectedKits.filter { (player, _) -> player.isOnline }.forEach { (onlinePlayer, kitType) ->
            addDwarf(onlinePlayer, kitType)
        }

        Bukkit.broadcast("<gray>Preparation phase has started, dwarfs prepare for battle!!!".withPrefix().mm())
        gameMap.dwarfSpawn.world.playSound(gameMap.dwarfSpawn, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f)

        super.onEnter()
    }

    private fun isActiveDwarf(player: Player): Boolean {
        return dwarfTeam.hasPlayer(player) && KitsManager.hasKit(player)
    }

    private fun lastActiveInRecruiting(player: Player): Boolean {
        return !dwarfTeam.hasPlayer(player) && selectedKits.containsKey(player)
    }

    private fun addDwarf(player: Player, kitType: KitType) {
        player.resetAll()
        player.scoreboard = customBoard
        dwarfTeam.addPlayer(player)
        player.teleport(gameMap.dwarfSpawn)
        KitsManager.setKit(player, kitType)
    }

    private fun openKitSelectionMenu(player: Player) {
        val basicDwarfKits = KitType.entries
            .filter { !it.isHero && it.team == Team.DWARF }
            .map {
                createItem(it.displayData.icon) {
                    name = it.displayData.name
                    lore(it.displayData.description.map(String::mm))
                    persistentDataContainer.set(it.key, PersistentDataType.BOOLEAN, false)
                }
            }

        object : PagerMenu(basicDwarfKits, player, "<gray><b>Select kit") {
            override fun handleClick(slot: Int, player: Player) {
                super.handleClick(slot, player)

                menu.getItem(slot)?.let { item ->
                    val keys = item.persistentDataContainer.keys
                    if (keys.isEmpty()) return@handleClick

                    KitType.getByKey(keys.first())?.let { type ->
                        addDwarf(player, type)
                    }
                }

            }
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.scoreboard = customBoard

        if (!isActiveDwarf(player)) {
            refreshToLobbyState(player)
        } else if (lastActiveInRecruiting(player)) {
            addDwarf(player, selectedKits[player]!!)
        }
    }

    @EventHandler
    fun onToolClick(event: PlayerInteractEvent) {
        handleLobbyToolClick(event, ::openKitSelectionMenu)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        handleLobbyInteract(event)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onEqInteract(event: InventoryClickEvent) {
        handleLobbyInvClick(event)
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        handleLobbyDamage(event)
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        handleLobbyItemDrop(event)
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.foodLevel = 20
    }

}