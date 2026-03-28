package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.dwarf.Archer
import com.jackob.dvz.kits.dwarf.Warrior
import com.jackob.dvz.storage.KitDisplay
import com.jackob.dvz.storage.KitDisplaysStorage
import org.bukkit.NamespacedKey

enum class KitType(
    val kitClass: Class<out BaseKit>,
    val team: Team,
    val isHero: Boolean,
    val displayData: KitDisplay
) {
    WARRIOR(Warrior::class.java, Team.DWARF, false, KitDisplaysStorage.getKitDisplayData("warrior")),
    ARCHER(Archer::class.java, Team.DWARF, false, KitDisplaysStorage.getKitDisplayData("archer")),
    ;

    val key: NamespacedKey by lazy {
        NamespacedKey(DvZ.INSTANCE, this.name.lowercase())
    }

    companion object {
        fun getByKey(key: NamespacedKey): KitType? {
            if (key.namespace != DvZ.INSTANCE.name.lowercase()) return null

            return try {
                KitType.valueOf(key.key.uppercase())
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}