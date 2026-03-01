package com.jackob.dvZ

import org.bukkit.plugin.java.JavaPlugin

class DvZ : JavaPlugin() {

    override fun onEnable() {
        logger.info("DvZ is enabled!")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
