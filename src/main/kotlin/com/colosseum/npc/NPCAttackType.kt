package com.colosseum.npc

/**
 * Enumeration of available NPC attack types with spawn probability
 */
enum class NPCAttackType(
    val probability: Double,
    val commandName: String,
) {
    SWORD(0.16, "sword"),
    BOW(0.16, "bow"),
    AXE(0.16, "axe"),
    FIREBALL(0.2, "fireball"),
    TRIDENT(0.08, "trident"),
    CROSSBOW(0.08, "crossbow"),
    SHIELD_SWORD(0.08, "shield"),
    POLEARM(0.08, "polearm"),

    ;

    companion object {
        fun fromCommandName(name: String): NPCAttackType? =
            entries.find { it.commandName == name.lowercase() }

        val validCommandNames: String
            get() = entries.joinToString(", ") { "'${it.commandName}'" }
    }
}
