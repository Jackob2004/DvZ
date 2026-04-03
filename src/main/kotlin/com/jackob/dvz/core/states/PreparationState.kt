package com.jackob.dvz.core.states

import com.jackob.dvz.kits.KitType
import com.jackob.dvz.kits.KitsManager
import com.jackob.dvz.kits.Team
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.resetAll
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team as BukkitTeam

class PreparationState(private val gameMap: GameMap, private val selectedKits: Map<Player, KitType>) : GameState {

    private val customBoard = Bukkit.getScoreboardManager().newScoreboard

    private val dwarfTeam = customBoard.registerNewTeam(Team.DWARF.teamName).apply {
        setAllowFriendlyFire(false)
        setOption(BukkitTeam.Option.COLLISION_RULE, BukkitTeam.OptionStatus.NEVER)
        color(Team.DWARF.color)
    }

    override fun onEnter() {
        selectedKits.filter { (player, _) -> player.isOnline }.forEach { (player, kitType) ->
            player.resetAll()
            player.scoreboard = customBoard
            dwarfTeam.addPlayer(player)
            player.teleport(gameMap.dwarfSpawn)
            KitsManager.setKit(player, kitType)
        }

        Bukkit.broadcast("<gray>Preparation phase has started, dwarfs prepare for battle!!!".withPrefix().mm())
        gameMap.dwarfSpawn.world.playSound(gameMap.dwarfSpawn, Sound.ITEM_GOAT_HORN_SOUND_0, 1f, 1f)

        super.onEnter()
    }
}