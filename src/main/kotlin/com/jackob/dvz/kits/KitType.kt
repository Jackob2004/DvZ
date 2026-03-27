package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import com.jackob.dvz.storage.KitDisplay
import org.bukkit.NamespacedKey

enum class KitType(
    val kitClass: Class<out Kit>,
    val team: Team,
    val isHero: Boolean,
    val displayData: KitDisplay
) {
    ;
    val key: NamespacedKey by lazy {
        NamespacedKey(DvZ.INSTANCE, this.name.lowercase())
    }
}