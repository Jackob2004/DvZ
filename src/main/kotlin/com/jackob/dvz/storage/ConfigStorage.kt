package com.jackob.dvz.storage

import com.jackob.dvz.DvZ

object ConfigStorage {
    val REQUIRED_PLAYERS = DvZ.INSTANCE.config.getInt("required-players")
    val COUNTDOWN = DvZ.INSTANCE.config.getInt("countdown")
    val MAP_CHANGE_TIME_LIMIT = DvZ.INSTANCE.config.getInt("map-change-time-limit")
}