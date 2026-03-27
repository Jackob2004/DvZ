package com.jackob.dvz.kits

import com.jackob.dvz.storage.KitDisplay
import org.bukkit.NamespacedKey

enum class KitType(
    val kitClass: Class<out Kit>,
    val team: Team,
    val isHero: Boolean,
    val id: NamespacedKey,
    val displayData: KitDisplay
) {
}