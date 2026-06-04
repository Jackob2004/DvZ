package com.jackob.dvz.core.states

import com.jackob.dvz.DvZ
import com.jackob.dvz.core.handlers.GameplayMechanicsHandler
import com.jackob.dvz.core.handlers.LobbyRulesHandler
import com.jackob.dvz.core.handlers.LobbyStateHandler
import com.jackob.dvz.core.objects.DarknessTask
import com.jackob.dvz.core.objects.GoldVault
import com.jackob.dvz.core.objects.Team
import com.jackob.dvz.kits.TeamType
import com.jackob.dvz.storage.GameMap
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.withPrefix
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.CopyOnWriteArraySet

class AttackState(
    private val gameMap: GameMap,
    private val lobbyStateHandler: LobbyStateHandler,
    private val lobbyRulesHandler: LobbyRulesHandler,
    private val gameplayHandler: GameplayMechanicsHandler,
    private val goldVault: GoldVault,
    private val darknessTask: DarknessTask,
    private val dwarfTeam: Team
) : GameState {

    private val zombieTeam = Team(TeamType.ZOMBIE)

    private val onlinePlayers = CopyOnWriteArraySet<Player>()

    override fun onEnter() {
        lobbyStateHandler.registerHandler(DvZ.INSTANCE)
        lobbyRulesHandler.registerHandler(DvZ.INSTANCE)
        gameplayHandler.registerHandler(DvZ.INSTANCE)

        onlinePlayers.addAll(Bukkit.getOnlinePlayers())
        darknessTask.startTask(onlinePlayers)

        // start plague
        Bukkit.broadcast("<gray>Attack phase has started, <dark_red>zombies have been released!!!".withPrefix().mm())

        super.onEnter()
    }

    override fun onLeave() {
        TODO("Not yet implemented")
        super.onLeave()
    }

    override fun getPlayerTeam(player: Player): TeamType? {
        var type: TeamType? = null
        if (dwarfTeam.hasMember(player)) {
            type = TeamType.DWARF
        } else if (zombieTeam.hasMember(player)) {
            type = TeamType.ZOMBIE
        }

        return type
    }

}