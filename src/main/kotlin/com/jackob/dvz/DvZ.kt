package com.jackob.dvz

import com.jackob.dvz.command.SaveLobbyCommand
import com.jackob.dvz.storage.MapStorage
import org.bukkit.plugin.java.JavaPlugin

class DvZ : JavaPlugin() {

    companion object {
        lateinit var INSTANCE: DvZ
    }

    override fun onEnable() {
        INSTANCE = this

        MapStorage

        registerCommand("dvz-set-lobby", SaveLobbyCommand())
        logger.info("DvZ is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
