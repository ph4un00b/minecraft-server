package com.colosseum.arena.commands

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for all command categories
 * Tests routing, validation, and integration
 */
class CommandCategoriesTest {

    // ============================================
    // Build Commands Tests
    // ============================================

    @Test
    fun `build commands - simple command routes correctly`() {
        val input = "simple"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SIMPLE, cmd)
        assertTrue(isBuildCommand(cmd))
    }

    @Test
    fun `build commands - detailed command routes correctly`() {
        val input = "detailed"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.DETAILED, cmd)
        assertTrue(isBuildCommand(cmd))
    }

    @Test
    fun `build commands - rebuild command routes correctly`() {
        val input = "rebuild"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.REBUILD, cmd)
        assertTrue(isBuildCommand(cmd))
    }

    @Test
    fun `build commands - sety command routes correctly`() {
        val input = "sety"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SET_Y, cmd)
        assertTrue(isBuildCommand(cmd))
    }

    @Test
    fun `build commands - aliases work`() {
        assertEquals(ArenaCommand.SIMPLE, ArenaCommand.fromString("s"))
        assertEquals(ArenaCommand.DETAILED, ArenaCommand.fromString("d"))
        assertEquals(ArenaCommand.REBUILD, ArenaCommand.fromString("r"))
        assertEquals(ArenaCommand.SET_Y, ArenaCommand.fromString("set-y"))
    }

    // ============================================
    // Player Commands Tests
    // ============================================

    @Test
    fun `player commands - restock command routes correctly`() {
        val input = "restock"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.RESTOCK, cmd)
        assertTrue(isPlayerCommand(cmd))
    }

    @Test
    fun `player commands - arrows command routes correctly`() {
        val input = "arrows"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.ARROWS, cmd)
        assertTrue(isPlayerCommand(cmd))
    }

    @Test
    fun `player commands - aliases work`() {
        assertEquals(ArenaCommand.RESTOCK, ArenaCommand.fromString("rs"))
        assertEquals(ArenaCommand.ARROWS, ArenaCommand.fromString("ar"))
    }

    // ============================================
    // NPC Commands Tests
    // ============================================

    @Test
    fun `npc commands - npcs command routes correctly`() {
        val input = "npcs"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.NPCS, cmd)
        assertTrue(isNPCCommand(cmd))
    }

    @Test
    fun `npc commands - togglenpcs command routes correctly`() {
        val input = "togglenpcs"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.TOGGLE_NPCS, cmd)
        assertTrue(isNPCCommand(cmd))
    }

    @Test
    fun `npc commands - setnpchealth command routes correctly`() {
        val input = "setnpchealth"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SET_NPC_HEALTH, cmd)
        assertTrue(isNPCCommand(cmd))
    }

    @Test
    fun `npc commands - setnpcdamage command routes correctly`() {
        val input = "setnpcdamage"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SET_NPC_DAMAGE, cmd)
        assertTrue(isNPCCommand(cmd))
    }

    @Test
    fun `npc commands - setnpccount command routes correctly`() {
        val input = "setnpccount"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SET_NPC_COUNT, cmd)
        assertTrue(isNPCCommand(cmd))
    }

    @Test
    fun `npc commands - setnpcattack command routes correctly`() {
        val input = "setnpcattack"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SET_NPC_ATTACK, cmd)
        assertTrue(isNPCCommand(cmd))
    }

    @Test
    fun `npc commands - aliases work`() {
        assertEquals(ArenaCommand.NPCS, ArenaCommand.fromString("npc"))
        assertEquals(ArenaCommand.TOGGLE_NPCS, ArenaCommand.fromString("toggle-npcs"))
        assertEquals(ArenaCommand.SET_NPC_HEALTH, ArenaCommand.fromString("set-npc-health"))
        assertEquals(ArenaCommand.SET_NPC_HEALTH, ArenaCommand.fromString("npchealth"))
        assertEquals(ArenaCommand.SET_NPC_DAMAGE, ArenaCommand.fromString("set-npc-damage"))
        assertEquals(ArenaCommand.SET_NPC_DAMAGE, ArenaCommand.fromString("npcdamage"))
        assertEquals(ArenaCommand.SET_NPC_COUNT, ArenaCommand.fromString("set-npc-count"))
        assertEquals(ArenaCommand.SET_NPC_COUNT, ArenaCommand.fromString("npccount"))
        assertEquals(ArenaCommand.SET_NPC_ATTACK, ArenaCommand.fromString("set-npc-attack"))
        assertEquals(ArenaCommand.SET_NPC_ATTACK, ArenaCommand.fromString("npcattack"))
    }

    // ============================================
    // Info Commands Tests
    // ============================================

    @Test
    fun `info commands - spawns command routes correctly`() {
        val input = "spawns"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.SPAWNS, cmd)
        assertTrue(isInfoCommand(cmd))
    }

    @Test
    fun `info commands - version command routes correctly`() {
        val input = "version"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.VERSION, cmd)
        assertTrue(isInfoCommand(cmd))
    }

    @Test
    fun `info commands - help command routes correctly`() {
        val input = "help"
        val cmd = ArenaCommand.fromString(input)
        assertEquals(ArenaCommand.HELP, cmd)
        assertTrue(isInfoCommand(cmd))
    }

    @Test
    fun `info commands - aliases work`() {
        assertEquals(ArenaCommand.SPAWNS, ArenaCommand.fromString("spawn"))
        assertEquals(ArenaCommand.VERSION, ArenaCommand.fromString("v"))
        assertEquals(ArenaCommand.HELP, ArenaCommand.fromString("h"))
        assertEquals(ArenaCommand.HELP, ArenaCommand.fromString("?"))
    }

    // ============================================
    // Command Generation Tests
    // ============================================

    @Test
    fun `generate usage string includes all primary names`() {
        val usage = ArenaCommand.generateUsageString()
        assertTrue(usage.contains("simple"))
        assertTrue(usage.contains("detailed"))
        assertTrue(usage.contains("rebuild"))
        assertTrue(usage.contains("sety"))
        assertTrue(usage.contains("restock"))
        assertTrue(usage.contains("arrows"))
        assertTrue(usage.contains("npcs"))
        assertTrue(usage.contains("version"))
        assertTrue(usage.contains("help"))
    }

    @Test
    fun `generate help text includes all commands`() {
        val helpText = ArenaCommand.generateHelpText()
        assertEquals(ArenaCommand.entries.size, helpText.size)

        // Check that each command has description
        ArenaCommand.entries.forEach { cmd ->
            val found = helpText.any { it.contains(cmd.primaryName) && it.contains(cmd.description) }
            assertTrue(found, "Help text should contain command '${cmd.primaryName}' with description")
        }
    }

    @Test
    fun `generate command usage formats correctly`() {
        val simpleUsage = ArenaCommand.generateCommandUsage(ArenaCommand.SIMPLE)
        assertEquals("Usage: /arena simple", simpleUsage)

        val setYUsage = ArenaCommand.generateCommandUsage(ArenaCommand.SET_Y)
        assertEquals("Usage: /arena sety <y-level>", setYUsage)

        val restockUsage = ArenaCommand.generateCommandUsage(ArenaCommand.RESTOCK)
        assertEquals("Usage: /arena restock [player]", restockUsage)
    }

    @Test
    fun `generate unknown option message lists all commands`() {
        val message = ArenaCommand.generateUnknownOptionMessage()
        assertTrue(message.startsWith("Unknown option. Use:"))
        // Should list primary names
        assertTrue(message.contains("simple"))
        assertTrue(message.contains("detailed"))
    }

    // ============================================
    // Edge Cases
    // ============================================

    @Test
    fun `fromString returns null for invalid command`() {
        assertNull(ArenaCommand.fromString("invalid"))
        assertNull(ArenaCommand.fromString(""))
        assertNull(ArenaCommand.fromString("   "))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(ArenaCommand.SIMPLE, ArenaCommand.fromString("SIMPLE"))
        assertEquals(ArenaCommand.SIMPLE, ArenaCommand.fromString("Simple"))
        assertEquals(ArenaCommand.SIMPLE, ArenaCommand.fromString("SiMpLe"))
    }

    @Test
    fun `all commands have unique primary names`() {
        val primaryNames = ArenaCommand.entries.map { it.primaryName }
        val uniqueNames = primaryNames.toSet()
        assertEquals(primaryNames.size, uniqueNames.size, "All primary names should be unique")
    }

    @Test
    fun `all commands have at least one alias`() {
        ArenaCommand.entries.forEach { cmd ->
            assertTrue(cmd.aliases.isNotEmpty(), "Command '${cmd.primaryName}' should have at least one alias")
        }
    }

    @Test
    fun `primary name is always first in aliases`() {
        ArenaCommand.entries.forEach { cmd ->
            assertEquals(cmd.primaryName, cmd.aliases.first(), "Primary name should be first alias for '${cmd.primaryName}'")
        }
    }

    // ============================================
    // Helper Methods
    // ============================================

    private fun isBuildCommand(cmd: ArenaCommand?): Boolean {
        return cmd in listOf(ArenaCommand.SIMPLE, ArenaCommand.DETAILED, ArenaCommand.REBUILD, ArenaCommand.SET_Y)
    }

    private fun isPlayerCommand(cmd: ArenaCommand?): Boolean {
        return cmd in listOf(ArenaCommand.RESTOCK, ArenaCommand.ARROWS)
    }

    private fun isNPCCommand(cmd: ArenaCommand?): Boolean {
        return cmd in listOf(
            ArenaCommand.NPCS, ArenaCommand.TOGGLE_NPCS,
            ArenaCommand.SET_NPC_HEALTH, ArenaCommand.SET_NPC_DAMAGE,
            ArenaCommand.SET_NPC_COUNT, ArenaCommand.SET_NPC_ATTACK
        )
    }

    private fun isInfoCommand(cmd: ArenaCommand?): Boolean {
        return cmd in listOf(ArenaCommand.SPAWNS, ArenaCommand.VERSION, ArenaCommand.HELP)
    }
}
