package com.colosseum.arena.domain

/**
 * Enumeration of available NPC attack types with spawn probability
 */
enum class NPCAttackType(val probability: Double) {
    SWORD(0.2),
    BOW(0.2),
    AXE(0.2),
    TRIDENT(0.1),
    CROSSBOW(0.1),
    SHIELD_SWORD(0.1),
    POLEARM(0.1),
}
