package com.colosseum.arena.commands

import com.colosseum.commands.infrastructure.CommandDisplay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
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
    fun `displayAllCommands has consistent line widths within box`() {
        val logger = Logger.getLogger("TestLogger10")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        val boxWidth = 58

        // Filter to only box content lines (those containing ║)
        val boxLines = handler.messages.filter { it.contains('║') }

        assertTrue(boxLines.isNotEmpty(), "Should have box lines")

        boxLines.forEach { line ->
            // Extract content between the box borders
            val start = line.indexOf('║')
            val end = line.lastIndexOf('║')

            if (start >= 0 && end > start) {
                val content = line.substring(start + 1, end)
                val contentWidth = content.visualWidth()

                assertEquals(
                    boxWidth,
                    contentWidth,
                    "Box content should be exactly $boxWidth columns " +
                        "but was $contentWidth: '$content'",
                )
            }
        }
    }

    @Test
    fun `displayAllCommands matches reference file`() {
        val logger = Logger.getLogger("TestLoggerRef")
        val handler = TestLogHandler()
        logger.addHandler(handler)
        logger.level = Level.ALL

        val display = CommandDisplay(logger)
        display.displayAllCommands()

        // Read reference file
        val testDir = "src/test/kotlin/com/colosseum/arena/commands/"
        val refFile = java.io.File("${testDir}expected_commands_display.txt")
        val referenceLines = refFile.readLines()

        // Get actual output and clean it (remove ANSI codes and prefix)
        val actualLines = handler.messages
            .filter {
                it.contains('║') ||
                    it.contains('╔') ||
                    it.contains('╚') ||
                    it.contains('╠')
            }
            .map { line ->
                // Remove ANSI color codes
                line.replace(Regex("\\u001B\\[[0-9;]*m"), "")
                    // Remove [ArenaPlugin] prefix
                    .replace("[ArenaPlugin] ", "")
            }

        // Build diff output
        val diffBuilder = StringBuilder()
        diffBuilder.appendLine("=== DIFF (Expected vs Actual) ===")
        diffBuilder.appendLine()

        val maxLines = maxOf(referenceLines.size, actualLines.size)
        var hasDiff = false

        for (i in 0 until maxLines) {
            val expected = referenceLines.getOrElse(i) { "" }
            val actual = actualLines.getOrElse(i) { "" }

            if (expected != actual) {
                hasDiff = true
                diffBuilder.appendLine("@@ Line ${i + 1} @@")
                diffBuilder.appendLine("- $expected")
                diffBuilder.appendLine("+ $actual")
                diffBuilder.appendLine()
            }
        }

        // Check line count mismatch
        if (referenceLines.size != actualLines.size) {
            hasDiff = true
            val refSize = referenceLines.size
            val actualSize = actualLines.size
            diffBuilder.appendLine(
                "@@ Line count mismatch: expected $refSize, got $actualSize @@",
            )
        }

        if (hasDiff) {
            fail<Nothing>(diffBuilder.toString())
        }
    }

    /**
     * Calculate visual width accounting for emojis
     */
    private fun String.visualWidth(): Int {
        // Count grapheme clusters (simplified: count emojis as 2 columns)
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
                // Regular characters
                else -> {
                    width += 1
                    i += Character.charCount(codePoint)
                }
            }
        }
        return width
    }
}
