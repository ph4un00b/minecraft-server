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
     */
    fun build(world: World, config: ArenaConfig)
}
