package com.jackob.dvz.setup

import com.jackob.dvz.DvZ
import io.papermc.paper.event.player.PlayerCustomClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object MapFormsManager : Listener {

    init {
        DvZ.INSTANCE.server.pluginManager.registerEvents(this, DvZ.INSTANCE)
    }

    private val forms: MutableMap<FormType, CustomForm> = HashMap(FormType.entries.size)

    init {
        forms[FormType.SETUP_FORM] = MapSetupForm()
    }

    fun getForm(formType: FormType): CustomForm {
        return forms[formType]!!
    }

    @Suppress("UnstableApiUsage")
    @EventHandler
    fun onCustomClick(event: PlayerCustomClickEvent) {
        val key = event.identifier.asString()
        forms[FormType.fromKey(key.substringBefore(":") + ":")]?.handleClick(event, key.substringAfter(":"))
    }

}