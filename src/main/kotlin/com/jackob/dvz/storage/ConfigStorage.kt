package com.jackob.dvz.storage

import com.jackob.dvz.DvZ

object ConfigStorage {
    val REQUIRED_PLAYERS = DvZ.INSTANCE.config.getInt("required-players")
    val RECRUITING_COUNTDOWN = DvZ.INSTANCE.config.getInt("recruiting-countdown")
    val PREPARATION_COUNTDOWN = DvZ.INSTANCE.config.getInt("preparation-countdown")
    val MAP_CHANGE_TIME_LIMIT = DvZ.INSTANCE.config.getInt("map-change-time-limit")
    val PLAYERS_PER_HERO = DvZ.INSTANCE.config.getInt("players-per-hero")
    val HERO_SELECT_TIME = DvZ.INSTANCE.config.getInt("hero-select-time")
    val RESTART_COUNTDOWN = DvZ.INSTANCE.config.getInt("restart-countdown")
    val GOLD_COLLECT_BASELINE = DvZ.INSTANCE.config.getInt("gold-collect-baseline")
    val ARMOR_REPAIR_COST = DvZ.INSTANCE.config.getInt("armor-repair-cost")
    val AI_ZOMBIE_MULTIPLIER = DvZ.INSTANCE.config.getDouble("ai-zombie-multiplier")
    val ZOMBIE_WAVE_INTERVAL = DvZ.INSTANCE.config.getInt("zombie-wave-interval")
}