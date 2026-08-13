package com.jackob.dvz

import com.github.retrooper.packetevents.PacketEvents
import com.jackob.dvz.command.DeleteMapCommand
import com.jackob.dvz.command.MapRerollCommand
import com.jackob.dvz.command.MapSetCommand
import com.jackob.dvz.command.SaveLobbyCommand
import com.jackob.dvz.command.SetupMapCommand
import com.jackob.dvz.command.TestCommand
import com.jackob.dvz.command.ZombieReleaseCommand
import com.jackob.dvz.core.GameManager
import com.jackob.dvz.core.equipment.EquipmentRegister
import com.jackob.dvz.kits.KitConfigsCache
import com.jackob.dvz.storage.ConfigStorage
import com.jackob.dvz.storage.KitDisplaysStorage
import com.jackob.dvz.ui.CustomMenuListener
import com.jackob.dvz.storage.MapStorage
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.plugin.java.JavaPlugin

class DvZ : JavaPlugin() {

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings
            .reEncodeByDefault(false)
            .checkForUpdates(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        PacketEvents.getAPI().init()
        INSTANCE = this

        saveDefaultConfig()
        MapStorage.cleanMapCopies()

        ConfigStorage
        GameManager
        KitDisplaysStorage
        KitConfigsCache

        registerCommand("dvz-set-lobby", SaveLobbyCommand())
        registerCommand("dvz-setup-map", SetupMapCommand())
        registerCommand("dvz-delete-map", DeleteMapCommand())
        registerCommand("dvz-map-reroll", MapRerollCommand())
        registerCommand("dvz-map-set", MapSetCommand())
        registerCommand("dvz-release-zombie", ZombieReleaseCommand())
        registerCommand("dvz-test", TestCommand())

        server.pluginManager.registerEvents(CustomMenuListener(), this)
        EquipmentRegister.initRegister()
        ConfigStorage.registerSkinIfNeeded(this, "elf", "elf.png")
        ConfigStorage.registerSkinIfNeeded(this, "shaman", "shaman.png")
        logger.info("DvZ is enabled!")
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }

    companion object {
        lateinit var INSTANCE: DvZ
    }
}
