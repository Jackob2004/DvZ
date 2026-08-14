package com.jackob.dvz.kits.dwarf.hero

import com.jackob.dvz.DvZ
import com.jackob.dvz.kits.BaseKit
import com.jackob.dvz.kits.Disguisable
import com.jackob.dvz.util.CombinationUtil
import com.jackob.dvz.util.CombinationUtil.ClickType.*
import com.jackob.dvz.util.CombinationUtil.Sequence
import com.jackob.dvz.util.ManaUtil
import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.name
import com.jackob.dvz.util.toPlayer
import com.jackob.dvz.util.withMana
import me.libraryaddict.disguise.disguisetypes.Disguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class Shaman(internalName: String, owner: UUID, isHero: Boolean) : BaseKit(internalName, owner, isHero),
    Disguisable<LivingWatcher> {

    override val disguiseTemplate: Disguise = createPlayerDisguise("shaman", "Shaman") {
        setItemStack(EquipmentSlot.HEAD, hiddenArmorPiece)
        setItemStack(EquipmentSlot.CHEST, hiddenArmorPiece)
        setItemStack(EquipmentSlot.LEGS, hiddenArmorPiece)
        setItemStack(EquipmentSlot.FEET, hiddenArmorPiece)
    }

    private val manaBank = ManaUtil(owner, 100, SHAMAN_STAFF)

    private val staffCombinations = CombinationUtil(owner, SHAMAN_STAFF).apply {
        registerAction(Sequence(RIGHT, LEFT, RIGHT), ::spawnTotem)
        registerAction(Sequence(RIGHT, RIGHT, RIGHT), ::leapTowardsTotem)
        registerAction(Sequence(RIGHT, LEFT, LEFT), ::pushEnemies)
        registerAction(Sequence(RIGHT, RIGHT, LEFT), ::pullEnemies)
    }

    companion object {
        private val hiddenArmorPiece = ItemStack(Material.AIR)
        private val SHAMAN_STAFF = NamespacedKey(DvZ.INSTANCE, "shaman-staff-item")

        private val shamanStaff = createItem(Material.WOODEN_HOE) {
            name = "<dark_green>Shaman Staff"
            description = """
                ? 
            """
            persistentDataContainer.set(ManaUtil.MANA_ITEM, PersistentDataType.BOOLEAN, true)
            persistentDataContainer.set(SHAMAN_STAFF, PersistentDataType.BOOLEAN, true)
        }
    }

    override fun onActivate() {
        super.onActivate()

        val player = ownerId.toPlayer()!!
        startDisguise(player)
        player.inventory.addItem(shamanStaff)
    }

    override fun onDeactivate() {
        super.onDeactivate()
        val player = ownerId.toPlayer()!!

        stopDisguise(player)
        manaBank.unregisterManaBank()
        staffCombinations.unregisterCombinations()
    }

    private fun spawnTotem(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, 500) {
        sendMessage("Spawning totem")
    }

    private fun leapTowardsTotem(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, 300) {
        sendMessage("Leaping towards totem")
    }

    private fun pushEnemies(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, 200) {
        sendMessage("Pushing enemies")
    }

    private fun pullEnemies(shamanPlayer: Player) = shamanPlayer.withMana(manaBank, 100) {
        sendMessage("Pulling enemies")
    }

}