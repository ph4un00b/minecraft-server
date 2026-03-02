package com.colosseum.arena.commands

/**
 * Enumeration of all available arena commands
 * Provides type-safe command matching with aliases support
 */
enum class ArenaCommand(
    val primaryName: String,
    val aliases: List<String>,
    val description: String,
    val usageParams: String = ""
) {
    ARROWS("arrows", listOf("arrows", "ar"), "Show arrow status"),
    CANCEL("cancel", listOf("cancel", "c"), "Cancel pending destructive command"),
    DETAILED("detailed", listOf("detailed", "d"), "Build detailed arena"),
    HELP("help", listOf("help", "h", "?"), "Show this help message"),
    NPCS("npcs", listOf("npcs", "npc"), "Show NPC status"),
    REBUILD("rebuild", listOf("rebuild", "r"), "Rebuild current arena"),
    RESTOCK("restock", listOf("restock", "rs"), "Restock arrows", "[player]"),
    SET_NPC_ATTACK("setnpcattack", listOf("setnpcattack", "set-npc-attack", "npcattack"), "Set NPC attack type", "<arrow|fireball>"),
    SET_NPC_COUNT("setnpccount", listOf("setnpccount", "set-npc-count", "npccount"), "Set NPC count", "<0-4>"),
    SET_NPC_DAMAGE("setnpcdamage", listOf("setnpcdamage", "set-npc-damage", "npcdamage"), "Set NPC damage", "<damage>"),
    SET_NPC_HEALTH("setnpchealth", listOf("setnpchealth", "set-npc-health", "npchealth"), "Set NPC health", "<health>"),
    SET_Y("sety", listOf("sety", "set-y"), "Change arena Y level", "<y-level>"),
    SIMPLE("simple", listOf("simple", "s"), "Build simple arena"),
    SPAWNS("spawns", listOf("spawns", "spawn"), "Show spawn info"),
    TOGGLE_NPCS("togglenpcs", listOf("togglenpcs", "toggle-npcs"), "Toggle NPCs on/off"),
    VERSION("version", listOf("version", "v"), "Show plugin version");

    companion object {
        /**
         * Message prefix for all plugin output
         */
        const val PREFIX = "\u001B[32m[ArenaPlugin]\u001B[0m "

        /**
         * Parse a string command into an ArenaCommand enum value
         * Case-insensitive matching against all aliases
         * @return The matching ArenaCommand or null if not found
         */
        fun fromString(input: String): ArenaCommand? {
            val normalized = input.lowercase()
            return entries.find { cmd ->
                cmd.aliases.any { alias -> alias == normalized }
            }
        }

        /**
         * Get all primary command names sorted alphabetically
         * @return List of primary command names
         */
        fun getPrimaryNames(): List<String> = entries
            .sortedBy { it.primaryName }
            .map { it.primaryName }

        /**
         * Generate usage string for command help
         * Format: /arena [ cmd1 | cmd2 | cmd3 | ... ]
         */
        fun generateUsageString(): String = getPrimaryNames()
            .joinToString(" | ")

        /**
         * Generate "Unknown option" help message
         */
        fun generateUnknownOptionMessage(): String =
            "Unknown option. Use: ${getPrimaryNames().joinToString(", ")}"

        /**
         * Generate help text with command descriptions
         * Sorted alphabetically by command name
         */
        fun generateHelpText(): List<String> = entries
            .sortedBy { it.primaryName }
            .map { "  /arena ${it.primaryName} - ${it.description}" }

        /**
         * Generate usage string for a specific command
         * Format: Usage: /arena <command> <params>
         * @param command The command to generate usage for
         * @return The formatted usage string
         */
        fun generateCommandUsage(command: ArenaCommand): String {
            return if (command.usageParams.isEmpty()) {
                "Usage: /arena ${command.primaryName}"
            } else {
                "Usage: /arena ${command.primaryName} ${command.usageParams}"
            }
        }
    }
}
