package com.colosseum.arena.commands

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * Tests for CommandDisplay - verifies all commands are displayed correctly
 */
class CommandDisplayTest {
    /**
     * Test helper that captures log messages
     */
    private class TestLogHandler : Handler() {
        val messages = mutableListOf<String>()

        override fun publish(record: LogRecord) {
            messages.add(record.message)
        }

        override fun flush() {}

        override fun close() {}
    }

    @Test
    fun `displayAllCommands includes all ArenaCommand entries`() {
        val logger = Logger.getLogger("TestLogger")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify every ArenaCommand is present
        ArenaCommand.entries.forEach { cmd ->
            assertTrue(
                loggedMessages.contains("/arena ${cmd.primaryName}"),
                "Output should contain command '/arena ${cmd.primaryName}'",
            )
        }
    }

    @Test
    fun `displayAllCommands shows all command descriptions`() {
        val logger = Logger.getLogger("TestLogger2")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify every command description is present
        ArenaCommand.entries.forEach { cmd ->
            assertTrue(
                loggedMessages.contains(cmd.description),
                "Output should contain description '${cmd.description}' for command ${cmd.primaryName}",
            )
        }
    }

    @Test
    fun `displayAllCommands shows command aliases`() {
        val logger = Logger.getLogger("TestLogger3")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify aliases section is present
        assertTrue(
            loggedMessages.contains("Aliases:"),
            "Output should contain 'Aliases:' section",
        )

        // Verify some specific aliases are shown
        ArenaCommand.entries.forEach { cmd ->
            val aliasesString = cmd.aliases.joinToString(", ")
            assertTrue(
                loggedMessages.contains(aliasesString),
                "Output should contain aliases '$aliasesString' for command ${cmd.primaryName}",
            )
        }
    }

    @Test
    fun `displayAllCommands uses purple color codes`() {
        val logger = Logger.getLogger("TestLogger4")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify purple ANSI color code is used
        assertTrue(
            loggedMessages.contains("\u001B[95m"),
            "Output should contain purple color code (\\u001B[95m)",
        )

        // Verify reset code is used
        assertTrue(
            loggedMessages.contains("\u001B[0m"),
            "Output should contain reset code (\\u001B[0m)",
        )
    }

    @Test
    fun `displayAllCommands shows header and footer`() {
        val logger = Logger.getLogger("TestLogger6")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify header is present
        assertTrue(
            loggedMessages.contains("AVAILABLE ARENA COMMANDS"),
            "Output should contain header 'AVAILABLE ARENA COMMANDS'",
        )

        // Verify footer with help message is present
        assertTrue(
            loggedMessages.contains("Use /arena help"),
            "Output should contain footer 'Use /arena help'",
        )
    }

    @Test
    fun `displayAllCommands includes correct command count`() {
        val logger = Logger.getLogger("TestLogger7")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        // Count lines that contain individual command entries ("/arena " followed by command name)
        // Filter out the footer line that contains "Use /arena help"
        val commandCount =
            handler.messages.count { line ->
                ArenaCommand.entries.any { cmd ->
                    line.contains("/arena ${cmd.primaryName}") && !line.contains("Use /arena help")
                }
            }

        // Should have all ArenaCommand entries
        assertEquals(
            ArenaCommand.entries.size,
            commandCount,
            "Output should contain exactly ${ArenaCommand.entries.size} command entries, but found $commandCount",
        )
    }

    @Test
    fun `displayAllCommands shows usage parameters for commands that have them`() {
        val logger = Logger.getLogger("TestLogger8")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Find commands that have usage parameters
        val commandsWithParams = ArenaCommand.entries.filter { it.usageParams.isNotEmpty() }

        // Verify those parameters are shown in output
        commandsWithParams.forEach { cmd ->
            assertTrue(
                loggedMessages.contains(cmd.usageParams),
                "Output should contain usage params '${cmd.usageParams}' for command ${cmd.primaryName}",
            )
        }
    }

    @Test
    fun `displayAllCommands shows category headers`() {
        val logger = Logger.getLogger("TestLogger9")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify all category headers are present
        assertTrue(
            loggedMessages.contains("BUILD COMMANDS"),
            "Output should contain 'BUILD COMMANDS' category",
        )
        assertTrue(
            loggedMessages.contains("PLAYER COMMANDS"),
            "Output should contain 'PLAYER COMMANDS' category",
        )
        assertTrue(
            loggedMessages.contains("NPC COMMANDS"),
            "Output should contain 'NPC COMMANDS' category",
        )
        assertTrue(
            loggedMessages.contains("INFO COMMANDS"),
            "Output should contain 'INFO COMMANDS' category",
        )
        assertTrue(
            loggedMessages.contains("UTILITY COMMANDS"),
            "Output should contain 'UTILITY COMMANDS' category",
        )
    }

    @Test
    fun `displayAllCommands groups build commands together`() {
        val logger = Logger.getLogger("TestLogger10")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val messages = handler.messages

        // Find the indices of category headers
        val buildIndex = messages.indexOfFirst { it.contains("BUILD COMMANDS") }
        val playerIndex = messages.indexOfFirst { it.contains("PLAYER COMMANDS") }

        assertTrue(buildIndex >= 0, "Should find BUILD COMMANDS header")
        assertTrue(playerIndex > buildIndex, "PLAYER COMMANDS should come after BUILD COMMANDS")

        // Verify build commands appear between build header and next category
        val buildCommands = listOf("simple", "detailed", "rebuild", "sety")
        buildCommands.forEach { cmdName ->
            val cmdIndex = messages.indexOfFirst { it.contains("/arena $cmdName") }
            assertTrue(
                // cmdIndex > buildIndex && cmdIndex < playerIndex,
                cmdIndex > buildIndex && cmdIndex < playerIndex,
                "Build command '/arena $cmdName' should appear in BUILD COMMANDS section",
            )
        }
    }

    @Test
    fun `displayAllCommands groups npc commands together`() {
        val logger = Logger.getLogger("TestLogger11")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val messages = handler.messages

        // Find the indices of category headers
        val npcIndex = messages.indexOfFirst { it.contains("NPC COMMANDS") }
        val infoIndex = messages.indexOfFirst { it.contains("INFO COMMANDS") }

        assertTrue(npcIndex >= 0, "Should find NPC COMMANDS header")
        assertTrue(infoIndex > npcIndex, "INFO COMMANDS should come after NPC COMMANDS")

        // Verify NPC commands appear between NPC header and next category
        val npcCommands = listOf("npcs", "togglenpcs", "setnpchealth", "setnpcdamage", "setnpccount", "setnpcattack")
        npcCommands.forEach { cmdName ->
            val cmdIndex = messages.indexOfFirst { it.contains("/arena $cmdName") }
            assertTrue(
                cmdIndex > npcIndex && cmdIndex < infoIndex,
                "NPC command '/arena $cmdName' should appear in NPC COMMANDS section",
            )
        }
    }

    @Test
    fun `displayAllCommands shows all categories in correct order`() {
        val logger = Logger.getLogger("TestLogger12")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val messages = handler.messages

        // Get indices of all category headers
        val indices =
            listOf(
                "BUILD COMMANDS" to messages.indexOfFirst { it.contains("BUILD COMMANDS") },
                "PLAYER COMMANDS" to messages.indexOfFirst { it.contains("PLAYER COMMANDS") },
                "NPC COMMANDS" to messages.indexOfFirst { it.contains("NPC COMMANDS") },
                "INFO COMMANDS" to messages.indexOfFirst { it.contains("INFO COMMANDS") },
                "UTILITY COMMANDS" to messages.indexOfFirst { it.contains("UTILITY COMMANDS") },
            )

        // Verify all categories are found and in ascending order
        indices.forEachIndexed { i, (name, index) ->
            assertTrue(index >= 0, "Should find category '$name'")
            if (i > 0) {
                assertTrue(
                    index > indices[i - 1].second,
                    "Category '$name' should come after '${indices[i - 1].first}'",
                )
            }
        }
    }
}
