package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.dwarf.Archer
import com.jackob.dvz.kits.dwarf.Warrior
import com.jackob.dvz.kits.dwarf.hero.Elf
import com.jackob.dvz.kits.zombie.base.Zombie
import com.jackob.dvz.kits.zombie.special.Enderman
import com.jackob.dvz.kits.zombie.special.SuperCreeper
import com.jackob.dvz.storage.KitDisplay
import com.jackob.dvz.storage.KitDisplaysStorage
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

enum class KitType(
    val kitClass: Class<out BaseKit>,
    val team: TeamType,
    val isHero: Boolean,
    val displayData: KitDisplay
) {
    WARRIOR(Warrior::class.java, TeamType.DWARF, false, KitDisplaysStorage.getKitDisplayData("warrior")),
    ARCHER(Archer::class.java, TeamType.DWARF, false, KitDisplaysStorage.getKitDisplayData("archer")),
    ELF(Elf::class.java, TeamType.DWARF, true, KitDisplaysStorage.getKitDisplayData("elf")),
    ZOMBIE(Zombie::class.java, TeamType.ZOMBIE, false, KitDisplaysStorage.getKitDisplayData("zombie")),
    SUPER_CREEPER(SuperCreeper::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("super_creeper")),
    ENDERMAN(Enderman::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("enderman"));

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

    fun toItem(configure: ItemMeta.() -> Unit = {}) : ItemStack {
        return createItem(displayData.icon) {
            name = displayData.name
            lore(displayData.description.map(String::mm))
            persistentDataContainer.set(key, PersistentDataType.BOOLEAN, false)
            this.configure()
        }
    }

}