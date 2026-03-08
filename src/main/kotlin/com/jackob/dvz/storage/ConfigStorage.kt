package com.jackob.dvz.storage

import com.jackob.dvz.DvZ

object ConfigStorage {
    val REQUIRED_PLAYERS = DvZ.INSTANCE.config.getInt("required-players")
}