package com.jackob.dvz.ui

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.score.ScoreFormat
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore
import com.jackob.dvz.util.mm
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class Sidebar private constructor(private val objectiveName: String) {

    private val linesMap: MutableMap<Int, SidebarLine> = HashMap()

    private val manager = PacketEvents.getAPI().playerManager

    private fun createLinePacket(
        lineNumber: Int,
        lineContent: Component,
    ): WrapperPlayServerUpdateScore {
        return WrapperPlayServerUpdateScore(
            lineNumber.toString(),
            WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
            objectiveName, lineNumber, lineContent,
            ScoreFormat.blankScore()
        )
    }

    private fun sendAllLines(receivers: Collection<Player>) {
        for ((key, value) in linesMap) {
            val packet = createLinePacket(key, value.lineContent)
            for (player in receivers) {
                if (!player.isOnline) continue
                manager.sendPacket(player, packet)
            }
        }
    }

    private fun sendUpdatedLines(receivers: Collection<Player>) {
        for ((key, value) in linesMap) {
            if (value.wasUpdated) {
                val packet = createLinePacket(key, value.lineContent)
                for (player in receivers) {
                    if (!player.isOnline) continue
                    manager.sendPacket(player, packet)
                }
                value.wasUpdated = false
            }
        }
    }

    fun line(lineNumber: Int, prefix: String = "", suffix: String = "") {
        linesMap[lineNumber] = SidebarLine(prefix, (prefix + suffix).mm())
    }

    fun updateLine(lineNumber: Int, suffix: String) {
        require(lineNumber in linesMap) { "($lineNumber) - This sidebar line doesn't exist!!!" }

        linesMap[lineNumber]!!.apply {
            wasUpdated = true
            lineContent = (prefix + suffix).mm()
        }
    }

    fun sendSidebar(players: Collection<Player>) {
        val objectivePacket = WrapperPlayServerScoreboardObjective(
            this@Sidebar.objectiveName,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
            this@Sidebar.objectiveName.mm(),
            WrapperPlayServerScoreboardObjective.RenderType.INTEGER
        )

        val displayPacket = WrapperPlayServerDisplayScoreboard(1, this@Sidebar.objectiveName)

        for (player in players) {
            if (!player.isOnline) continue
            manager.sendPacket(player, objectivePacket)
            manager.sendPacket(player, displayPacket)
        }

        sendAllLines(players)
    }

    fun sendSideBarUpdate(players: Collection<Player>) {
        sendUpdatedLines(players)
    }

    companion object {
        fun create(sidebarName: String, init: Sidebar.() -> Unit): Sidebar {
            val sidebar = Sidebar(sidebarName)
            sidebar.init()
            return sidebar
        }
    }

    private data class SidebarLine(val prefix: String, var lineContent: Component, var wasUpdated: Boolean = false)
}