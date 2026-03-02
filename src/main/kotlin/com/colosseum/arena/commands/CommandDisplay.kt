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
    private val boxWidth = 58

    /**
     * Calculate visual width accounting for emojis (which are double-width)
     * Handles variation selectors (U+FE0F) and emoji sequences properly
     */
    private fun String.visualWidth(): Int {
        var width = 0
        var i = 0
        while (i < this.length) {
            val codePoint = this.codePointAt(i)
            when {
                // Emojis and supplementary characters (double-width)
                codePoint > 0xFFFF -> {
                    width += 2
                    i += Character.charCount(codePoint)
                }
                // Variation selectors (0xFE00-0xFE0F) - skip, they're part of emoji
                codePoint in 0xFE00..0xFE0F -> {
                    i += Character.charCount(codePoint)
                }
                // Regular characters (single-width)
                else -> {
                    width += 1
                    i += Character.charCount(codePoint)
                }
            }
        }
        return width
    }

    /**
     * Command categories for grouping display
     */
    private enum class Category(
        val displayName: String,
        val commands: List<ArenaCommand>,
    ) {
        BUILD(
            "🏗️ BUILD COMMANDS",
            listOf(
                ArenaCommand.SIMPLE,
                ArenaCommand.DETAILED,
                ArenaCommand.REBUILD,
                ArenaCommand.SET_Y,
            ),
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
            "ℹ️ INFO COMMANDS",
            listOf(
                ArenaCommand.SPAWNS,
                ArenaCommand.VERSION,
                ArenaCommand.HELP,
            ),
        ),
        UTILITY(
            "⚙️ UTILITY COMMANDS",
            listOf(ArenaCommand.CANCEL),
        ),
    }

    /**
     * Log all available commands in a formatted purple box grouped by category
     */
    fun displayAllCommands() {
        val line = "═".repeat(boxWidth)
        val dashLine = "─".repeat(boxWidth)
        val emptyLine = " ".repeat(boxWidth)

        logger.info("$cmdPrefix╔$line╗$reset")

        val header = "AVAILABLE ARENA COMMANDS"
        val headerPadding = " ".repeat(boxWidth - header.length - 1)
        logger.info("$cmdPrefix║ $header$headerPadding║$reset")

        logger.info("$cmdPrefix╠$dashLine╣$reset")

        Category.entries.forEach { category ->
            // Category header
            logger.info("$cmdPrefix║$emptyLine║$reset")

            val catPadding = " ".repeat(
                boxWidth - 1 - category.displayName.visualWidth(),
            )
            logger.info(
                "$cmdPrefix║ ${category.displayName}$catPadding║$reset",
            )

            val separator = "═".repeat(category.displayName.visualWidth())
            val sepPadding = " ".repeat(boxWidth - 1 - separator.visualWidth())
            logger.info("$cmdPrefix║ $separator$sepPadding║$reset")

            // Commands in this category
            category.commands.forEach { cmd ->
                val aliases = cmd.aliases.joinToString(", ")
                val usage =
                    if (cmd.usageParams.isNotEmpty()) {
                        " ${cmd.usageParams}"
                    } else {
                        ""
                    }

                val cmdLine = "    /arena ${cmd.primaryName}$usage"
                val cmdPadding = " ".repeat(boxWidth - cmdLine.length)
                logger.info("$cmdPrefix║$cmdLine$cmdPadding║$reset")

                val aliasLine = "       Aliases: $aliases"
                val aliasPadding = " ".repeat(boxWidth - aliasLine.length)
                logger.info("$cmdPrefix║$aliasLine$aliasPadding║$reset")

                val descLine = "       ${cmd.description}"
                val descPadding = " ".repeat(boxWidth - descLine.length)
                logger.info("$cmdPrefix║$descLine$descPadding║$reset")

                logger.info("$cmdPrefix║$emptyLine║$reset")
            }

            // Separator between categories
            if (category != Category.entries.last()) {
                logger.info("$cmdPrefix╠$dashLine╣$reset")
            }
        }

        logger.info("$cmdPrefix║$emptyLine║$reset")

        val helpMsg = "Use /arena help for detailed usage information - phau"
        val helpPadding = " ".repeat(boxWidth - 1 - helpMsg.length)
        logger.info("$cmdPrefix║ $helpMsg$helpPadding║$reset")

        logger.info("$cmdPrefix╚$line╝$reset")
    }
}
