package com.jackob.dvz

import com.jackob.dvz.command.DeleteMapCommand
import com.jackob.dvz.command.MapRerollCommand
import com.jackob.dvz.command.MapSetCommand
import com.jackob.dvz.command.SaveLobbyCommand
import com.jackob.dvz.command.SetupMapCommand
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.ui.CustomMenuListener
import com.jackob.dvz.storage.MapStorage
import org.bukkit.plugin.java.JavaPlugin

class DvZ : JavaPlugin() {

    override fun onEnable() {
        INSTANCE = this

        saveDefaultConfig()

        ConfigStorage
        MapStorage
        GameManager

        registerCommand("dvz-set-lobby", SaveLobbyCommand())
        registerCommand("dvz-setup-map", SetupMapCommand())
        registerCommand("dvz-delete-map", DeleteMapCommand())
        registerCommand("dvz-map-reroll", MapRerollCommand())
        registerCommand("dvz-map-set", MapSetCommand())
        server.pluginManager.registerEvents(CustomMenuListener(), this)
        logger.info("DvZ is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    companion object {
        lateinit var INSTANCE: DvZ
    }
}
