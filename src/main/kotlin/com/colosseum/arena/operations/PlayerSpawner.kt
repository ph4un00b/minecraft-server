package com.colosseum.arena.operations

import com.colosseum.arena.domain.SpawnPoint
import com.colosseum.arena.domain.SpawnPosition
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import kotlin.math.cos
import kotlin.math.sin

/**
 * PlayerSpawner - Handles player spawn point rotation and marker building
 *
 * 4 fixed spawn positions at inner edge (radius 12): East, South, West, North
 * Rotation: East → South → West → North (clockwise)
 * Each spawn has decorative floor markers
 */
class PlayerSpawner {
    companion object {
        private const val SPAWN_RADIUS = 12 // Inner edge of arena
        private const val ARROWS_PER_PLAYER = 5
    }

    // Track next spawn index: 0=E, 1=S, 2=W, 3=N
    private var nextSpawnIndex = 0

    /**
     * Get the next spawn point for a player (rotates clockwise: E, S, W, N)
     * Also builds safety floor at the spawn location
     */
    fun getNextSpawnPoint(
        world: World,
        baseY: Int,
    ): Location {
        val spawnPosition = SpawnPosition.getByIndex(nextSpawnIndex)
        nextSpawnIndex = (nextSpawnIndex + 1) % 4

        val spawnPoint = calculateSpawnPoint(world, spawnPosition, baseY)

        // Ensure safety floor at this spawn
        buildSafetyFloor(world, spawnPoint, baseY)

        return spawnPoint.toLocation(world)
    }

    /**
     * Calculate spawn point coordinates at inner edge
     */
    private fun calculateSpawnPoint(
        world: World,
        position: SpawnPosition,
        baseY: Int,
    ): SpawnPoint {
        val angleRad = Math.toRadians(position.angleDegrees)
        val x = (cos(angleRad) * SPAWN_RADIUS).toInt()
        val z = (sin(angleRad) * SPAWN_RADIUS).toInt()

        return SpawnPoint(position, x, baseY + 1, z)
    }

    /**
     * Build a safety floor at the spawn point (3x3 platform)
     */
    private fun buildSafetyFloor(
        world: World,
        spawnPoint: SpawnPoint,
        floorY: Int,
    ) {
        for (dx in -1..1) {
            for (dz in -1..1) {
                val block = world.getBlockAt(spawnPoint.x + dx, floorY, spawnPoint.z + dz)
                if (block.type == Material.AIR || block.type == Material.VOID_AIR) {
                    block.type = Material.STONE_BRICKS
                }
            }
        }
    }

    /**
     * Build all spawn point floor markers during arena construction
     */
    fun buildSpawnMarkers(
        world: World,
        baseY: Int,
    ) {
        SpawnPosition.getAll().forEach { position ->
            val spawnPoint = calculateSpawnPoint(world, position, baseY)

            // Create 3x3 marked area with different material per direction
            val markerMaterial =
                when (position) {
                    SpawnPosition.EAST -> Material.GOLD_BLOCK // Gold for East
                    SpawnPosition.SOUTH -> Material.DIAMOND_BLOCK // Diamond for South
                    SpawnPosition.WEST -> Material.EMERALD_BLOCK // Emerald for West
                    SpawnPosition.NORTH -> Material.LAPIS_BLOCK // Lapis for North
                }

            // Build 3x3 platform at spawn location
            for (dx in -1..1) {
                for (dz in -1..1) {
                    val block = world.getBlockAt(spawnPoint.x + dx, baseY, spawnPoint.z + dz)
                    // Use marker material for center
                    if (dx == 0 && dz == 0) {
                        block.type = markerMaterial
                    } else if ((dx == 0 || dz == 0)) {
                        // Cardinal directions around center
                        block.type = Material.POLISHED_ANDESITE
                    } else {
                        // Corners
                        block.type = Material.STONE_BRICKS
                    }
                }
            }

            // Add a small indicator block above
            val indicator = world.getBlockAt(spawnPoint.x, baseY + 2, spawnPoint.z)
            indicator.type =
                when (position) {
                    SpawnPosition.EAST -> Material.TORCH
                    SpawnPosition.SOUTH -> Material.REDSTONE_TORCH
                    SpawnPosition.WEST -> Material.SOUL_TORCH
                    SpawnPosition.NORTH -> Material.LANTERN
                }
        }
    }

    /**
     * Reset spawn rotation (useful after arena rebuild)
     */
    fun resetRotation() {
        nextSpawnIndex = 0
    }

    /**
     * Get spawn location name based on coordinates
     */
    fun getSpawnLocationName(
        x: Int,
        z: Int,
    ): String {
        return when {
            x > 5 && z in -5..5 -> "East Spawn (Gold)"
            x < -5 && z in -5..5 -> "West Spawn (Emerald)"
            z > 5 && x in -5..5 -> "South Spawn (Diamond)"
            z < -5 && x in -5..5 -> "North Spawn (Lapis)"
            else -> "Arena Center"
        }
    }
}
