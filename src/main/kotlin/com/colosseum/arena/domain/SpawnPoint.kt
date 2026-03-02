package com.colosseum.arena.domain

import org.bukkit.Location
import org.bukkit.World

/**
 * Represents a fixed spawn point in the arena
 * 4 positions: East (0), South (1), West (2), North (3)
 */
enum class SpawnPosition(
    val index: Int,
    val displayName: String,
    val angleDegrees: Double,
) {
    EAST(0, "East", 90.0),
    SOUTH(1, "South", 180.0),
    WEST(2, "West", 270.0),
    NORTH(3, "North", 0.0),
    ;

    companion object {
        private val values = entries.toTypedArray()

        fun getByIndex(index: Int): SpawnPosition {
            return values[index % values.size]
        }

        fun getAll(): Array<SpawnPosition> = values
    }
}

/**
 * Spawn point data with coordinates
 */
data class SpawnPoint(
    val position: SpawnPosition,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun toLocation(world: World): Location {
        return Location(world, x + 0.5, y.toDouble(), z + 0.5)
    }
}
