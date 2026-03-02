package com.colosseum.arena.domain

/**
 * Immutable arena configuration
 * Passed to builders to configure the build
 */
data class ArenaConfig(
    val baseY: Int,
    val type: ArenaType,
)
