package com.colosseum.arena.commands

import java.util.logging.Logger

/**
 * Displays all available arena commands in brilliant purple color
 * Formats command information with aliases and descriptions grouped by category
 */
class CommandDisplay(private val logger: Logger) {
    private val purple = "\u001B[95m" // Bright magenta/purple
    private val reset = "\u001B[0m"
    private val cmdPrefix = "$purple[ArenaPlugin] "

    /**
     * Command categories for grouping display
     */
    private enum class Category(val displayName: String, val commands: List<ArenaCommand>) {
        BUILD(
            "🏗️  BUILD COMMANDS",
            listOf(ArenaCommand.SIMPLE, ArenaCommand.DETAILED, ArenaCommand.REBUILD, ArenaCommand.SET_Y),
        ),
        PLAYER(
            "🎯 PLAYER COMMANDS",
            listOf(ArenaCommand.RESTOCK, ArenaCommand.ARROWS),
        ),
        NPC(
            "🤖 NPC COMMANDS",
            listOf(
                ArenaCommand.NPCS,
                ArenaCommand.TOGGLE_NPCS,
                ArenaCommand.SET_NPC_HEALTH,
                ArenaCommand.SET_NPC_DAMAGE,
                ArenaCommand.SET_NPC_COUNT,
                ArenaCommand.SET_NPC_ATTACK,
            ),
        ),
        INFO(
            "ℹ️  INFO COMMANDS",
            listOf(ArenaCommand.SPAWNS, ArenaCommand.VERSION, ArenaCommand.HELP),
        ),
        UTILITY(
            "⚙️  UTILITY COMMANDS",
            listOf(ArenaCommand.CANCEL),
        ),
    }

    /**
     * Log all available commands in a formatted purple box grouped by category
     */
    fun displayAllCommands() {
        logger.info("$cmdPrefix╔══════════════════════════════════════════════════════════╗$reset")
        logger.info("$cmdPrefix║           AVAILABLE ARENA COMMANDS                       ║$reset")
        logger.info("$cmdPrefix╠══════════════════════════════════════════════════════════╣$reset")

        Category.entries.forEach { category ->
            // Category header
            logger.info("$cmdPrefix║                                                          ║$reset")
            logger.info("$cmdPrefix║  ${category.displayName}$reset")
            logger.info("$cmdPrefix║  ${"═".repeat(category.displayName.length)}$reset")

            // Commands in this category
            category.commands.forEach { cmd ->
                val aliases = cmd.aliases.joinToString(", ")
                val usage = if (cmd.usageParams.isNotEmpty()) " ${cmd.usageParams}" else ""
                logger.info("$cmdPrefix║    /arena ${cmd.primaryName}$usage$reset")
                logger.info("$cmdPrefix║       Aliases: $aliases$reset")
                logger.info("$cmdPrefix║       ${cmd.description}$reset")
                logger.info("$cmdPrefix║                                                          ║$reset")
            }

            // Separator between categories
            if (category != Category.entries.last()) {
                logger.info("$cmdPrefix╠──────────────────────────────────────────────────────────╣$reset")
            }
        }

        logger.info("$cmdPrefix║                                                          ║$reset")
        logger.info("$cmdPrefix║  Use /arena help for detailed usage information          ║$reset")
        logger.info("$cmdPrefix╚══════════════════════════════════════════════════════════╝$reset")
    }
}
