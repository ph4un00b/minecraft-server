package com.colosseum.combat.kit

import org.bukkit.enchantments.Enchantment

/**
 * Combat kit configuration constants
 * Bow has Power I and Unbreaking III ONLY - NO INFINITY
 * Player gets exactly 5 arrows, no unlimited arrows
 *
 * API Documentation:
 * - Enchantment: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html
 *   - POWER: Extra damage when shooting arrows (levels 1-5)
 *   - UNBREAKING: Decreases durability loss (levels 1-3)
 *   - INFINITY: Unlimited arrows (NOT used - we want finite arrows!)
 *
 * PaperMC Registry (1.21+):
 * - https://jd.papermc.io/paper/1.21/org/bukkit/enchantments/Enchantment.html
 *   - Registry-based enchantments for modern versions
 */
object KitConfig {
    // Arrow amounts
    const val INITIAL_ARROWS = 5
    const val RESTOCK_AMOUNT = 5
    const val MAX_ARROWS = 10

    // Bow enchantment levels
    const val POWER_LEVEL = 1
    const val UNBREAKING_LEVEL = 3

    /**
     * Get default bow enchantments
     * Can't be const val because it's a Map
     */
    val defaultBowEnchantments: Map<Enchantment, Int> =
        buildMap {
            put(Enchantment.POWER, POWER_LEVEL) // Power I - extra damage
            put(Enchantment.UNBREAKING, UNBREAKING_LEVEL) // Unbreaking III
            // NO INFINITY enchantment - arrows will run out!
        }
}
