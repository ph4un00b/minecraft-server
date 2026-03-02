package com.colosseum.arena.commands

import java.util.logging.Logger

/**
 * Displays all available arena commands in brilliant purple color
 * Formats command information with aliases and descriptions
 */
class CommandDisplay(private val logger: Logger) {

    private val purple = "\u001B[95m" // Bright magenta/purple
    private val reset = "\u001B[0m"
    private val cmdPrefix = "$purple[ArenaPlugin] "

    /**
     * Log all available commands in a formatted purple box
     */
    fun displayAllCommands() {
        logger.info("${cmdPrefix}╔══════════════════════════════════════════════════════════╗$reset")
        logger.info("${cmdPrefix}║           AVAILABLE ARENA COMMANDS                       ║$reset")
        logger.info("${cmdPrefix}╠══════════════════════════════════════════════════════════╣$reset")

        ArenaCommand.entries
            .sortedBy { it.primaryName }
            .forEach { cmd ->
                val aliases = cmd.aliases.joinToString(", ")
                val usage = if (cmd.usageParams.isNotEmpty()) " ${cmd.usageParams}" else ""
                logger.info("${cmdPrefix}║  /arena ${cmd.primaryName}$usage$reset")
                logger.info("${cmdPrefix}║     Aliases: $aliases$reset")
                logger.info("${cmdPrefix}║     ${cmd.description}$reset")
                logger.info("${cmdPrefix}╠──────────────────────────────────────────────────────────╣$reset")
            }

        logger.info("${cmdPrefix}║  Use /arena help for detailed usage information          ║$reset")
        logger.info("${cmdPrefix}╚══════════════════════════════════════════════════════════╝$reset")
    }
}
