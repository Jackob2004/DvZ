package com.jackob.dvz.core.statistics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerStatistics(
    val uuid: String,

    @SerialName("username")
    val name: String,

    @SerialName("experience_points")
    var experiencePoints: Int = 0,

    @SerialName("dwarf_kills")
    var dwarfKills: Int = 0,

    @SerialName("zombie_kills")
    var zombieKills: Int = 0,

    @SerialName("ai_zombie_kills")
    var aiZombieKills: Int = 0,

    @SerialName("shrine_kills")
    var shrineKills: Int = 0,

    @SerialName("deaths")
    var deaths: Int = 0,

    @SerialName("longest_time_survived_seconds")
    var longestTimeSurvived: Long = 0,

    @SerialName("total_play_time_seconds")
    var totalPlayTime: Long = 0,

    @SerialName("games_played")
    var gamesPlayed: Int = 0,

    @SerialName("rampage_kill_streak")
    var rampageKillStreak: Int = 0,

    @SerialName("bow_shoots")
    var bowShoots: Int = 0,

    @SerialName("bow_hits")
    var bowHits: Int = 0
)

@Serializable
data class LeaderboardRow(
    @SerialName("username")
    val name: String,

    val level: Int,
)

@Serializable
data class GameInfo(
    val gamePhase: String,
    val time: Int,
    val playersToStart: Int? = null,
    val dwarves: Int? = null,
    val zombies: Int? = null,
    val shrines: Int? = null,
    val currShrine: Int? = null,
    val shrineHealth: Int? = null,
    val shrineMaxHealth: Int? = null,
    val shrineShielded: Boolean? = null,
)