package com.jackob.dvz.kits

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.dwarf.base.Archer
import com.jackob.dvz.kits.dwarf.base.Builder
import com.jackob.dvz.kits.dwarf.base.Warrior
import com.jackob.dvz.kits.dwarf.hero.Elf
import com.jackob.dvz.kits.dwarf.hero.Shaman
import com.jackob.dvz.kits.dwarf.hero.Wizard
import com.jackob.dvz.kits.zombie.base.Zombie
import com.jackob.dvz.kits.zombie.special.Enderman
import com.jackob.dvz.kits.zombie.base.Creeper
import com.jackob.dvz.kits.zombie.base.Skeleton
import com.jackob.dvz.kits.zombie.special.IronGolem
import com.jackob.dvz.kits.zombie.special.Spider
import com.jackob.dvz.kits.zombie.special.SuperCreeper
import com.jackob.dvz.kits.zombie.special.Witch
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
    val displayData: KitDisplay,
    val waveSize: Float? = null
) {
    WARRIOR(Warrior::class.java, TeamType.DWARF, false, KitDisplaysStorage.getKitDisplayData("warrior")),
    ARCHER(Archer::class.java, TeamType.DWARF, false, KitDisplaysStorage.getKitDisplayData("archer")),
    BUILDER(Builder::class.java, TeamType.DWARF, false, KitDisplaysStorage.getKitDisplayData("builder")),
    ELF(Elf::class.java, TeamType.DWARF, true, KitDisplaysStorage.getKitDisplayData("elf")),
    SHAMAN(Shaman::class.java, TeamType.DWARF, true, KitDisplaysStorage.getKitDisplayData("shaman")),
    WIZARD(Wizard::class.java, TeamType.DWARF, true, KitDisplaysStorage.getKitDisplayData("wizard")),
    ZOMBIE(Zombie::class.java, TeamType.ZOMBIE, false, KitDisplaysStorage.getKitDisplayData("zombie")),
    SUPER_CREEPER(SuperCreeper::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("super_creeper"), 0.3f),
    ENDERMAN(Enderman::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("enderman"), 0.0f),
    SKELETON(Skeleton::class.java, TeamType.ZOMBIE, false, KitDisplaysStorage.getKitDisplayData("skeleton")),
    CREEPER(Creeper::class.java, TeamType.ZOMBIE, false, KitDisplaysStorage.getKitDisplayData("creeper")),
    IRON_GOLEM(IronGolem::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("iron_golem"), 0.1f),
    SPIDER(Spider::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("spider"), 0.4f),
    WITCH(Witch::class.java, TeamType.ZOMBIE, true, KitDisplaysStorage.getKitDisplayData("witch"), 0.3f);

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