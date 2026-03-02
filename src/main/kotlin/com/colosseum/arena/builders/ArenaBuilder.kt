package com.colosseum.arena.builders

import org.bukkit.World
import com.colosseum.arena.domain.ArenaConfig

/**
 * Interface for arena builders
 * Implementations contain their own geometric state (constants)
 */
interface ArenaBuilder {
    /**
     * Build the arena in the world with given configuration
     * @param world The world to build in
     * @param config The arena configuration
     * @param placer The block placer to use (immediate or queued)
     */
    fun build(world: World, config: ArenaConfig, placer: BlockPlacer = ImmediateBlockPlacer())

    /**
     * Build spawn markers for the arena
     */
    fun buildSpawnMarkers(world: World, baseY: Int)
}
