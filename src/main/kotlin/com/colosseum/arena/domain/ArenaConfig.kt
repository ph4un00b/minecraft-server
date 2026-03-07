package com.colosseum.arena.domain

import com.colosseum.arena.config.TargetBlockConfig

/**
 * Immutable arena configuration
 * Passed to builders to configure the build
 */
data class ArenaConfig(
    val baseY: Int,
    val type: ArenaType,
    val targetBlockConfig: TargetBlockConfig = TargetBlockConfig(),
)
