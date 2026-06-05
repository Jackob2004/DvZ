package com.jackob.dvz.core.objects

import com.jackob.dvz.kits.TeamType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.scoreboard.Team as BukkitTeam

class Team(teamType: TeamType) {

    private val team = customBoard.registerNewTeam(teamType.teamName).apply {
        setAllowFriendlyFire(false)
        setOption(BukkitTeam.Option.COLLISION_RULE, BukkitTeam.OptionStatus.NEVER)
        color(teamType.color)
    }

    private val onlineMembers = AtomicInteger()

    companion object {
        private val customBoard = Bukkit.getScoreboardManager().newScoreboard

        fun refreshTeamVisibility(player: Player) {
            player.scoreboard = customBoard
        }
    }

    fun addMember(player: Player) {
        player.scoreboard = customBoard
        team.addPlayer(player)
        onlineMembers.incrementAndGet()
    }

    fun removeMember(player: Player) {
        team.removePlayer(player)
        onlineMembers.decrementAndGet()
    }

    fun hasMember(player: Player): Boolean = team.hasPlayer(player)

    fun increaseOnlineCount() = onlineMembers.incrementAndGet()

    fun decreaseOnlineCount() = onlineMembers.decrementAndGet()

    fun getOnlineCount(): Int = onlineMembers.get()
}