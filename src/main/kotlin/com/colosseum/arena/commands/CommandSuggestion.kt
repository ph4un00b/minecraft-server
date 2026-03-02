package com.colosseum.arena.commands

/**
 * Utility class for command suggestions using Levenshtein distance algorithm
 * Extracted for testability and single responsibility
 */
object CommandSuggestion {
    /**
     * Suggest a similar command based on Levenshtein distance
     * @param input The user input to check
     * @return The closest matching command name or null if none close enough
     */
    fun suggestSimilar(input: String): String? {
        val normalized = input.lowercase()
        val threshold = 3

        return ArenaCommand.entries
            .map { cmd ->
                val distance = levenshteinDistance(normalized, cmd.primaryName)
                cmd.primaryName to distance
            }
            .filter { it.second <= threshold }
            .minByOrNull { it.second }
            ?.first
    }

    /**
     * Calculate Levenshtein distance between two strings
     * Returns minimum number of single-character edits needed
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        for (j in costs.indices) {
            costs[j] = j
        }

        for (i in s1.indices) {
            costs[0] = i + 1
            var nw = i
            for (j in s2.indices) {
                val cj =
                    minOf(
                        costs[j + 1] + 1,
                        costs[j] + 1,
                        nw + if (s1[i] == s2[j]) 0 else 1,
                    )
                nw = costs[j + 1]
                costs[j + 1] = cj
            }
        }
        return costs[s2.length]
    }
}
