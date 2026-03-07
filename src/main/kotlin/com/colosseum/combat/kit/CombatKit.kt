package com.colosseum.combat.kit

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * Combat kit management
 * Handles equipping players with bow and arrows, restocking, and repairs
 *
 * API Documentation:
 * - Material: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html
 *   - BOW, ARROW material types for items
 *
 * - ItemStack: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/ItemStack.html
 *   - Creating item stacks with specific materials and amounts
 *   - Managing item metadata and enchantments
 *
 * - ItemMeta: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/meta/ItemMeta.html
 *   - Adding enchantments to items
 *   - Modifying item properties
 *
 * - Damageable: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/meta/Damageable.html
 *   - Repairing items by setting damage to 0
 *
 * - Player Inventory: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/inventory/PlayerInventory.html
 *   - Clearing inventory, adding items, setting main hand
 */
class CombatKit(private val config: KitConfig) {
    /**
     * Equip player with fresh combat kit
     * Clears inventory, gives enchanted bow + 5 arrows
     */
    fun equipPlayer(player: Player) {
        val inventory = player.inventory

        // Clear inventory for fresh start
        inventory.clear()

        // Create enchanted bow
        val bow = ItemStack(Material.BOW)
        val bowMeta = bow.itemMeta
        config.bowEnchantments.forEach { (enchantment, level) ->
            bowMeta.addEnchant(enchantment, level, true)
        }
        bow.itemMeta = bowMeta

        // Give bow
        inventory.setItemInMainHand(bow)

        // Give initial arrows
        inventory.addItem(ItemStack(Material.ARROW, config.initialArrows))
    }

    /**
     * Restock player with arrows and repair bow
     * Adds 5 arrows (capped at 10 max) and repairs bow to full durability
     *
     * @param player The player to restock
     * @return true if restocked successfully, false if no bow found
     */
    fun restockPlayer(player: Player): Boolean {
        val inventory = player.inventory

        // Find and repair bow
        var bowFound = false
        for (item in inventory.contents) {
            if (item != null && item.type == Material.BOW) {
                // Repair bow to full durability
                val meta = item.itemMeta
                if (meta is Damageable) {
                    meta.damage = 0
                    item.itemMeta = meta
                }
                bowFound = true
            }
        }

        if (!bowFound) {
            return false
        }

        // Add arrows (capped at max)
        val currentArrows = getArrowCount(player)
        val arrowsToAdd =
            minOf(config.restockAmount, config.maxArrows - currentArrows)

        if (arrowsToAdd > 0) {
            inventory.addItem(ItemStack(Material.ARROW, arrowsToAdd))
        }

        return true
    }

    /**
     * Get current arrow count in player inventory
     */
    private fun getArrowCount(player: Player): Int {
        return player.inventory.contents
            .filterNotNull()
            .filter { it.type == Material.ARROW }
            .sumOf { it.amount }
    }

    /**
     * Check if player has a bow in inventory
     */
    fun hasBow(player: Player): Boolean {
        return player.inventory.contents.any {
            it != null && it.type == Material.BOW
        }
    }

    /**
     * Get configuration summary for display
     */
    fun getConfigSummary(): String {
        val enchants = config.bowEnchantments.map {
            "${it.key.key.key}:${it.value}"
        }.joinToString(",")
        val initial = config.initialArrows
        val restock = config.restockAmount
        val max = config.maxArrows
        return "bow=$enchants, initial=$initial, restock=$restock, max=$max"
    }
}
