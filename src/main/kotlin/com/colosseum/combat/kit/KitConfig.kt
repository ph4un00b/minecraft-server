package com.colosseum.combat.kit

import org.bukkit.enchantments.Enchantment

/**
 * Configuration for combat kit
 * Bow has Power I and Unbreaking III ONLY - NO INFINITY
 * Player gets exactly 5 arrows, no unlimited arrows
 *
 * API Documentation:
 * - Enchantment: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/enchantments/Enchantment.html
 *   - POWER: Extra damage when shooting arrows (levels 1-5)
 *   - UNBREAKING: Decreases durability loss (levels 1-3)
 *   - INFINITY: Unlimited arrows (NOT used - we want finite arrows!)
 *   - ARROW_DAMAGE: Alternative name for POWER in some versions
 *
 * PaperMC Registry (1.21+):
 * - https://jd.papermc.io/paper/1.21/org/bukkit/enchantments/Enchantment.html
 *   - Registry-based enchantments for modern versions
 */
data class KitConfig(
    val bowEnchantments: Map<Enchantment, Int> =
        buildMap {
            put(Enchantment.POWER, 1) // Power I - extra damage
            put(Enchantment.UNBREAKING, 3) // Unbreaking III - bow lasts longer
            // NO INFINITY enchantment - arrows will run out!
        },
    val initialArrows: Int = 5,
    val restockAmount: Int = 5,
    val maxArrows: Int = 10,
)
