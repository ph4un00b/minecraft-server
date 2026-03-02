package com.colosseum.arena.operations

import org.bukkit.Material
import org.bukkit.World

/**
 * Clears the arena area completely
 * Stateless operation
 */
class ArenaClearer {
    private val radius = 25

    /**
     * Clear the entire arena area from world bottom to top
     */
    fun clear(world: World) {
        val minY = world.minHeight
        val maxY = world.maxHeight.coerceAtMost(320)

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                for (y in minY..maxY) {
                    val block = world.getBlockAt(x, y, z)
                    // Remove everything except bedrock
                    if (block.type != Material.BEDROCK) {
                        block.type = Material.AIR
                    }
                }
            }
        }
    }
}
