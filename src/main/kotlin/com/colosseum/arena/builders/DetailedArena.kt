package com.colosseum.arena.builders

import org.bukkit.World
import org.bukkit.Material
import com.colosseum.arena.domain.ArenaConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Detailed gothic arena builder
 * Creates a thick wall with buttresses, windows, gate, and decorative elements
 */
class DetailedArena : ArenaBuilder {
    // Geometric constants (state)
    private val centerX = 0
    private val centerZ = 0
    private val innerRadius = 12.0
    private val outerRadius = 19.5
    private val wallHeight = 10

    override fun build(world: World, config: ArenaConfig) {
        val groundY = config.baseY
        val wallTop = groundY + wallHeight

        // Build all components
        buildGround(world, groundY)
        buildThickWall(world, groundY)
        buildButtresses(world, groundY)
        buildWindows(world, groundY)
        buildGate(world, groundY)
        buildCrenellations(world, wallTop)
        buildFloorPattern(world, groundY)
    }

    private fun buildGround(world: World, groundY: Int) {
        val radius = outerRadius.toInt() + 2
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance <= radius) {
                    val block = world.getBlockAt(centerX + x, groundY, centerZ + z)
                    block.type = if (distance <= 12) {
                        if ((x + z) % 2 == 0) Material.SMOOTH_STONE else Material.STONE_BRICKS
                    } else {
                        Material.GRASS_BLOCK
                    }
                }
            }
        }
    }

    private fun buildThickWall(world: World, groundY: Int) {
        val wallTop = groundY + wallHeight

        for (x in -outerRadius.toInt()..outerRadius.toInt()) {
            for (z in -outerRadius.toInt()..outerRadius.toInt()) {
                val distance = sqrt((x * x + z * z).toDouble())

                // Wall condition: 12.0 <= distance <= 19.5
                if (distance >= innerRadius && distance <= outerRadius) {
                    for (y in groundY + 1..wallTop) {
                        val block = world.getBlockAt(centerX + x, y, centerZ + z)

                        // Varied stone materials for texture
                        block.type = when {
                            distance > 18 -> Material.DEEPSLATE_BRICKS
                            distance > 15 -> Material.STONE_BRICKS
                            else -> Material.CRACKED_STONE_BRICKS
                        }
                    }
                }
            }
        }
    }

    private fun buildButtresses(world: World, groundY: Int) {
        // Buttresses at E, SE, S, SW, W, NW, NE (skip N for gate)
        val buttressAngles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 315.0)

        for (angleDeg in buttressAngles) {
            val angleRad = Math.toRadians(angleDeg)
            val x = (cos(angleRad) * outerRadius).toInt()
            val z = (sin(angleRad) * outerRadius).toInt()

            // Build buttress (3x3 base, tapering up)
            for (by in groundY + 1..groundY + wallHeight + 2) {
                val width = when {
                    by < groundY + 3 -> 2
                    by < groundY + 6 -> 1
                    else -> 0
                }

                for (bx in -width..width) {
                    for (bz in -width..width) {
                        val blockX = centerX + x + bx
                        val blockZ = centerZ + z + bz
                        val block = world.getBlockAt(blockX, by, blockZ)
                        block.type = Material.DEEPSLATE_BRICKS
                    }
                }
            }
        }
    }

    private fun buildWindows(world: World, groundY: Int) {
        // Windows at 22.5 degree offsets from buttresses
        val windowAngles = listOf(22.5, 67.5, 112.5, 157.5, 202.5, 247.5, 292.5, 337.5)

        for (angleDeg in windowAngles) {
            val angleRad = Math.toRadians(angleDeg)

            // Carve window through wall thickness
            for (r in innerRadius.toInt()..outerRadius.toInt()) {
                val x = (cos(angleRad) * r).toInt()
                val z = (sin(angleRad) * r).toInt()

                // Window height (gothic arch shape)
                for (wy in groundY + 2..groundY + 6) {
                    val isArch = wy == groundY + 6 && r > (innerRadius + outerRadius) / 2
                    if (!isArch) {
                        world.getBlockAt(centerX + x, wy, centerZ + z).type = Material.AIR
                    }
                }
            }
        }
    }

    private fun buildGate(world: World, groundY: Int) {
        // Gate at North (Z = -19, angle 270/-90 degrees)
        val gateZ = -outerRadius.toInt()
        val gateWidth = 5
        val gateHeight = 8

        // Carve opening
        for (x in -gateWidth..gateWidth) {
            for (y in groundY + 1..groundY + gateHeight) {
                for (z in gateZ..gateZ + 2) {
                    world.getBlockAt(centerX + x, y, centerZ + z).type = Material.AIR
                }
            }
        }

        // Build arch
        val archRadius = gateWidth + 1
        for (angle in 0..180) {
            val rad = Math.toRadians(angle.toDouble())
            val archX = (cos(rad) * archRadius).toInt()
            val archY = (sin(rad) * (gateHeight / 2)).toInt()

            world.getBlockAt(centerX + archX, groundY + gateHeight + archY, centerZ + gateZ).type =
                Material.CHISELED_STONE_BRICKS
        }

        // Gate towers
        for (towerX in listOf(-gateWidth - 1, gateWidth + 1)) {
            for (y in groundY + 1..groundY + wallHeight + 3) {
                for (z in gateZ..gateZ + 2) {
                    world.getBlockAt(centerX + towerX, y, centerZ + z).type = Material.DEEPSLATE_BRICKS
                }
            }
        }
    }

    private fun buildCrenellations(world: World, wallTop: Int) {
        // Add battlements every 2 blocks
        for (angle in 0 until 360 step 10) {
            val rad = Math.toRadians(angle.toDouble())

            // Outer crenellations
            val outerX = (cos(rad) * (outerRadius + 0.5)).toInt()
            val outerZ = (sin(rad) * (outerRadius + 0.5)).toInt()

            if (angle % 20 == 0) {
                world.getBlockAt(centerX + outerX, wallTop + 1, centerZ + outerZ).type = Material.STONE_BRICK_WALL
            }

            // Skip inner crenellations at gate
            val innerX = (cos(rad) * (innerRadius - 0.5)).toInt()
            val innerZ = (sin(rad) * (innerRadius - 0.5)).toInt()

            if (angle % 20 == 0 && innerZ > -18) {
                world.getBlockAt(centerX + innerX, wallTop + 1, centerZ + innerZ).type = Material.STONE_BRICK_WALL
            }
        }
    }

    private fun buildFloorPattern(world: World, groundY: Int) {
        // Create a decorative floor pattern in the arena
        val radius = innerRadius.toInt()
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val distance = sqrt((x * x + z * z).toDouble())
                if (distance <= radius) {
                    val block = world.getBlockAt(centerX + x, groundY, centerZ + z)

                    // Concentric circles pattern
                    val patternRadius = distance.toInt()
                    block.type = when {
                        patternRadius % 4 == 0 -> Material.POLISHED_ANDESITE
                        patternRadius % 2 == 0 -> Material.SMOOTH_STONE
                        (x + z) % 3 == 0 -> Material.MOSSY_STONE_BRICKS
                        else -> Material.STONE_BRICKS
                    }
                }
            }
        }

        // Center podium
        for (x in -2..2) {
            for (z in -2..2) {
                world.getBlockAt(centerX + x, groundY + 1, centerZ + z).type = Material.CHISELED_STONE_BRICKS
            }
        }
        world.getBlockAt(centerX, groundY + 2, centerZ).type = Material.LANTERN
    }
}
