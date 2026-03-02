package com.colosseum.arena.domain

object NPCDecisions {
    private const val MAX_NPCS = 4

    fun nextBatchSize(currentSize: Int): Int {
        return (currentSize + 1).coerceAtMost(MAX_NPCS)
    }
}
