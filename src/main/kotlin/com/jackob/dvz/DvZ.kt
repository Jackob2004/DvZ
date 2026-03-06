package com.jackob.dvz

import com.jackob.dvz.command.SaveLobbyCommand
import com.jackob.dvz.command.SetupMapCommand
import com.jackob.dvz.setup.BasicMapInfoForm
import com.jackob.dvz.ui.CustomMenuListener
import com.jackob.dvz.storage.MapStorage
import org.bukkit.plugin.java.JavaPlugin

class DvZ : JavaPlugin() {

    override fun onEnable() {
        INSTANCE = this

        MapStorage

        registerCommand("dvz-set-lobby", SaveLobbyCommand())
        registerCommand("dvz-setup-map", SetupMapCommand())
        server.pluginManager.registerEvents(CustomMenuListener(), this)
        server.pluginManager.registerEvents(BasicMapInfoForm(), this)
        logger.info("DvZ is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    companion object {
        lateinit var INSTANCE: DvZ
    }
}
