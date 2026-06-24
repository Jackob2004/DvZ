package com.jackob.dvz.core.objects

import com.jackob.dvz.kits.TeamType
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicInteger
import org.bukkit.scoreboard.Team as BukkitTeam

/**
 * @property onMemberQuit a fn that is called whenever online members count decreases
 */
class Team(teamType: TeamType, var onMemberQuit: ((Int) -> Unit)? = null) {

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
        decreaseOnlineCount()
    }

    fun recalculateOnlineCount() {
        val count = Bukkit.getOnlinePlayers().count { hasMember(it) }
        onlineMembers.set(count)
    }

    fun hasMember(player: Player): Boolean = team.hasPlayer(player)

    fun increaseOnlineCount() = onlineMembers.incrementAndGet()

    fun decreaseOnlineCount() {
        onlineMembers.decrementAndGet()
        onMemberQuit?.invoke(getOnlineCount())
    }

    fun getOnlineCount(): Int = onlineMembers.get()
}