package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.dwarf.Warrior
import com.jackob.dvz.storage.KitDisplay
import com.jackob.dvz.storage.KitDisplaysStorage
import org.bukkit.NamespacedKey

enum class KitType(
    val kitClass: Class<out Kit>,
    val team: Team,
    val isHero: Boolean,
    val displayData: KitDisplay
) {
    WARRIOR(Warrior::class.java, Team.DWARF, false, KitDisplaysStorage.getKitDisplayData("warrior")),
    ;

    val key: NamespacedKey by lazy {
        NamespacedKey(DvZ.INSTANCE, this.name.lowercase())
    }
}