package com.colosseum.arena.builders

import org.bukkit.Material
import org.bukkit.World

/**
 * Interface for placing blocks - can be immediate or queued for async
 */
interface BlockPlacer {
    fun setBlock(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        material: Material,
    )

    fun fillArea(
        world: World,
        x1: Int,
        y1: Int,
        z1: Int,
        x2: Int,
        y2: Int,
        z2: Int,
        material: Material,
    )

    fun getBlockCount(): Int
}

/**
 * Immediate block placer - places blocks synchronously (original behavior)
 */
class ImmediateBlockPlacer : BlockPlacer {
    override fun setBlock(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        material: Material,
    ) {
        world.getBlockAt(x, y, z).type = material
    }

    override fun fillArea(
        world: World,
        x1: Int,
        y1: Int,
        z1: Int,
        x2: Int,
        y2: Int,
        z2: Int,
        material: Material,
    ) {
        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)
        val minZ = minOf(z1, z2)
        val maxZ = maxOf(z1, z2)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    setBlock(world, x, y, z, material)
                }
            }
        }
    }

    override fun getBlockCount(): Int = 0 // Not tracked for immediate placement
}

/**
 * Queued block placer - stores blocks for async placement
 */
class QueuedBlockPlacer : BlockPlacer {
    data class BlockPlacement(val world: World, val x: Int, val y: Int, val z: Int, val material: Material)

    private val blocks = mutableListOf<BlockPlacement>()

    override fun setBlock(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        material: Material,
    ) {
        blocks.add(BlockPlacement(world, x, y, z, material))
    }

    override fun fillArea(
        world: World,
        x1: Int,
        y1: Int,
        z1: Int,
        x2: Int,
        y2: Int,
        z2: Int,
        material: Material,
    ) {
        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)
        val minZ = minOf(z1, z2)
        val maxZ = maxOf(z1, z2)

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    blocks.add(BlockPlacement(world, x, y, z, material))
                }
            }
        }
    }

    override fun getBlockCount(): Int = blocks.size

    fun getBlocks(): List<BlockPlacement> = blocks.toList()

    fun clear() = blocks.clear()
}
