package com.colosseum.npc

/**
 * NPC configuration constants
 * Central source of truth for all NPC-related constants
 */
object NPCConfig {
    // Default values
    const val DEFAULT_HEALTH = 1.0
    const val DEFAULT_DAMAGE = 5.0
    const val DEFAULT_COUNT = 1
    const val DEFAULT_ENABLED = true

    // Limits
    const val MIN_HEALTH = 1.0
    const val MIN_DAMAGE = 1.0
    const val MAX_COUNT = 4
    const val MIN_COUNT = 0
}
