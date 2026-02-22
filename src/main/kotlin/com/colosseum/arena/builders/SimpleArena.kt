package com.colosseum.arena.builders

import org.bukkit.World
import org.bukkit.Material
import com.colosseum.arena.domain.ArenaConfig
import kotlin.math.sqrt

/**
 * Simple arena builder
 * Creates a basic circular wall with single gate
 */
class SimpleArena : ArenaBuilder {
    // Geometric constants (state)
    private val centerX = 0
    private val centerZ = 0
    private val innerRadius = 12
    private val outerRadius = 18
    private val wallHeight = 6

    override fun build(world: World, config: ArenaConfig) {
        val groundY = config.baseY

        // Build ground
        for (x in -outerRadius..outerRadius) {
            for (z in -outerRadius..outerRadius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance <= outerRadius) {
                    world.getBlockAt(centerX + x, groundY, centerZ + z).type = Material.GRASS_BLOCK
                }
            }
        }

        // Build wall ring
        for (x in -outerRadius..outerRadius) {
            for (z in -outerRadius..outerRadius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance >= innerRadius && distance <= outerRadius) {
                    for (h in 0 until wallHeight) {
                        world.getBlockAt(centerX + x, groundY + 1 + h, centerZ + z).type = Material.STONE_BRICKS
                    }
                }
            }
        }

        // Create entrance (gate) at North
        for (x in -3..3) {
            for (y in groundY + 1..groundY + wallHeight) {
                for (z in -outerRadius - 1..-innerRadius + 1) {
                    world.getBlockAt(centerX + x, y, centerZ + z).type = Material.AIR
                }
            }
        }

        // Add gate arch
        for (x in -4..4) {
            world.getBlockAt(centerX + x, groundY + wallHeight + 1, centerZ - outerRadius).type = Material.STONE_BRICKS
        }
        for (y in groundY + 1..groundY + wallHeight + 1) {
            world.getBlockAt(centerX - 4, y, centerZ - outerRadius).type = Material.STONE_BRICKS
            world.getBlockAt(centerX + 4, y, centerZ - outerRadius).type = Material.STONE_BRICKS
        }
    }
}
