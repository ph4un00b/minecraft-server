package com.colosseum.arena.commands

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Integration tests for CommandSuggestion functionality
 * Tests the Levenshtein distance algorithm and command suggestion logic
 */
class CommandSuggestionTest {

    @Test
    fun `levenshtein distance - identical strings should return 0`() {
        val distance = CommandSuggestion.levenshteinDistance("simple", "simple")
        assertEquals(0, distance)
    }

    @Test
    fun `levenshtein distance - single character deletion should return 1`() {
        val distance = CommandSuggestion.levenshteinDistance("simpl", "simple")
        assertEquals(1, distance)
    }

    @Test
    fun `levenshtein distance - single character insertion should return 1`() {
        val distance = CommandSuggestion.levenshteinDistance("simplee", "simple")
        assertEquals(1, distance)
    }

    @Test
    fun `levenshtein distance - single character substitution should return 1`() {
        val distance = CommandSuggestion.levenshteinDistance("simpla", "simple")
        assertEquals(1, distance)
    }

    @Test
    fun `levenshtein distance - two transposed characters should return 2`() {
        val distance = CommandSuggestion.levenshteinDistance("resotck", "restock")
        assertEquals(2, distance)
    }

    @Test
    fun `levenshtein distance - completely different strings`() {
        val distance = CommandSuggestion.levenshteinDistance("xyz", "simple")
        assertTrue(distance > 3)
    }

    @Test
    fun `suggest similar command - typo should return correct suggestion`() {
        val suggestion = CommandSuggestion.suggestSimilar("simpl")
        assertEquals("simple", suggestion)
    }

    @Test
    fun `suggest similar command - another typo`() {
        val suggestion = CommandSuggestion.suggestSimilar("resotck")
        assertEquals("restock", suggestion)
    }

    @Test
    fun `suggest similar command - nonsense should return null`() {
        val suggestion = CommandSuggestion.suggestSimilar("xyzabc")
        assertNull(suggestion)
    }
}
