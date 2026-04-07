package com.jackob.dvz.core.handlers

import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

/**
 * Represents handler that acts as a component that can be injected in game state
 */
interface CoreHandler : Listener {
    fun registerHandler(plugin: JavaPlugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    fun unregisterHandler() {
        HandlerList.unregisterAll(this)
    }
}