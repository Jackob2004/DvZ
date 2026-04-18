package com.jackob.dvz.core.enchantments

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.registry.event.RegistryEvents


@Suppress("UnstableApiUsage")
class EnchantmentBootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        context.lifecycleManager.registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler { event ->
            CustomEnchantmentRegistry.enchantments.forEach {
                event.registry().register(
                    it.key,
                    it.buildEnchantment()
                )
            }
        })
    }

}