package com.jackob.dvz.ui

import com.jackob.dvz.util.createItem
import com.jackob.dvz.util.description
import com.jackob.dvz.util.mm
import com.jackob.dvz.util.name
import com.jackob.dvz.util.updateItem
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

private const val SIZE = 45
private const val ITEMS_PER_PAGE = SIZE - 2 * 9

private const val NEXT_PAGE_BTN_IDX = 44
private const val PREV_PAGE_BTN_IDX = 43
private const val INFO_IDX = 42

open class PagerMenu(
    private val contents: List<ItemStack>,
    val canDeactivate: Boolean = false,
    player: Player? = null,
    title: String
) : CustomMenu {

    private var currentPage = 0

    private val allPages = contents.size / ITEMS_PER_PAGE + if (contents.size % ITEMS_PER_PAGE != 0) 1 else 0

    protected val menu = Bukkit.createInventory(this, SIZE, title.mm()).apply {
        val fillerIcon = createItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE) {
            name = "<white>Use buttons at the bottom to navigate"
        }
        (0..8).forEach { setItem(it, fillerIcon) }
        (36..41).forEach { setItem(it, fillerIcon) }

        createItem(Material.TIPPED_ARROW) {
            name = "<blue><u>Next page"
        }.also { setItem(NEXT_PAGE_BTN_IDX, it) }

        createItem(Material.ARROW) {
            name = "<blue><u>Previous page"
        }.also { setItem(PREV_PAGE_BTN_IDX, it) }

        createItem(Material.PAPER) {
            name = "<gray>Info"
            description = """
                <gray>Current page: <white>${currentPage + 1}
                <gray>All pages: <white>$allPages
            """
        }.also { setItem(INFO_IDX, it) }
    }

    init {
        if (player != null) {
            rerender()
            player.openInventory(menu)
        }
    }

    companion object {
        fun safeDeactivate(player: Player) {
            val holder = player.openInventory.topInventory.holder

            if (holder is PagerMenu && holder.canDeactivate) {
                player.closeInventory()
            }
        }
    }

    private fun nextPage(): Boolean {
        currentPage++
        if (currentPage == allPages) {
            currentPage--
            return false
        }

        return true
    }

    private fun prevPage(): Boolean {
        currentPage--
        if (currentPage < 0) {
            currentPage = 0
            return false
        }

        return true
    }

    private fun currentPageContents(): List<ItemStack> {
        val firstIdx = currentPage * ITEMS_PER_PAGE
        val possibleLastIdx = firstIdx + ITEMS_PER_PAGE - 1
        val lastIdx = if (possibleLastIdx > contents.size - 1) contents.size - 1 else possibleLastIdx

        val itemsOnPage = mutableListOf<ItemStack>()

        for (i in firstIdx..lastIdx) {
            itemsOnPage.add(contents[i])
        }

        return itemsOnPage
    }

    private fun rerender() {
        val pageContents = currentPageContents()

        var contentsIdx = 0
        for (slotIdx in 9..35) {
            if (contentsIdx < pageContents.size) {
                menu.setItem(slotIdx, pageContents[contentsIdx])
            } else {
                menu.setItem(slotIdx, null)
            }

            contentsIdx++
        }

        menu.getItem(INFO_IDX)!!.updateItem {
            description = """
                <gray>Current page: <white>${currentPage + 1}
                <gray>All pages: <white>$allPages
            """
        }
    }

    fun open(player: Player) {
        rerender()
        player.openInventory(menu)
    }

    override fun handleClick(slot: Int, player: Player) {
        val shouldRerender: Boolean = when (slot) {
            NEXT_PAGE_BTN_IDX -> nextPage()
            PREV_PAGE_BTN_IDX -> prevPage()
            else -> return
        }

        if (shouldRerender) {
            rerender()
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
        } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
        }
    }

    override fun getInventory() = menu
}