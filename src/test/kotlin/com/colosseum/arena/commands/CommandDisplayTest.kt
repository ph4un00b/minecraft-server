package com.colosseum.arena.commands

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.LogRecord

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
                "Output should contain command '/arena ${cmd.primaryName}'"
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
                "Output should contain description '${cmd.description}' for command ${cmd.primaryName}"
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
            "Output should contain 'Aliases:' section"
        )

        // Verify some specific aliases are shown
        ArenaCommand.entries.forEach { cmd ->
            val aliasesString = cmd.aliases.joinToString(", ")
            assertTrue(
                loggedMessages.contains(aliasesString),
                "Output should contain aliases '$aliasesString' for command ${cmd.primaryName}"
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
            "Output should contain purple color code (\\u001B[95m)"
        )

        // Verify reset code is used
        assertTrue(
            loggedMessages.contains("\u001B[0m"),
            "Output should contain reset code (\\u001B[0m)"
        )
    }

    @Test
    fun `displayAllCommands formats box borders correctly`() {
        val logger = Logger.getLogger("TestLogger5")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val loggedMessages = handler.messages.joinToString("\n")

        // Verify box drawing characters are used
        assertTrue(
            loggedMessages.contains("╔"),
            "Output should contain top-left box corner"
        )
        assertTrue(
            loggedMessages.contains("╗"),
            "Output should contain top-right box corner"
        )
        assertTrue(
            loggedMessages.contains("╚"),
            "Output should contain bottom-left box corner"
        )
        assertTrue(
            loggedMessages.contains("╝"),
            "Output should contain bottom-right box corner"
        )
        assertTrue(
            loggedMessages.contains("║"),
            "Output should contain vertical border"
        )
        assertTrue(
            loggedMessages.contains("═"),
            "Output should contain horizontal border"
        )
        assertTrue(
            loggedMessages.contains("╠"),
            "Output should contain left T-junction"
        )
        assertTrue(
            loggedMessages.contains("╣"),
            "Output should contain right T-junction"
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
            "Output should contain header 'AVAILABLE ARENA COMMANDS'"
        )

        // Verify footer with help message is present
        assertTrue(
            loggedMessages.contains("Use /arena help"),
            "Output should contain footer 'Use /arena help'"
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
        val commandCount = handler.messages.count { line ->
            ArenaCommand.entries.any { cmd ->
                line.contains("/arena ${cmd.primaryName}") && !line.contains("Use /arena help")
            }
        }

        // Should have all ArenaCommand entries
        assertEquals(
            ArenaCommand.entries.size,
            commandCount,
            "Output should contain exactly ${ArenaCommand.entries.size} command entries, but found $commandCount"
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
                "Output should contain usage params '${cmd.usageParams}' for command ${cmd.primaryName}"
            )
        }
    }
}
