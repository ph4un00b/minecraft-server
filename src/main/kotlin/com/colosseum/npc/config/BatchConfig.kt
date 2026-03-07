package com.colosseum.npc.config

import com.colosseum.npc.NPCDecisions

/**
 * Configuration for batch spawning behavior
 * Central source of truth for all batch-related settings
 */
data class BatchConfig(
    val spawnMode: SpawnMode = SpawnMode.ON_TARGET_HIT,
    val startingBatchSize: Int = 1,
    val maxBatchSize: Int = 4,
    val progressionType: ProgressionType = ProgressionType.LINEAR,
    val infiniteBatches: Boolean = true,
    val spawnHostile: Boolean = true,
) {
    companion object {
        const val DEFAULT_MIN_BATCH_SIZE = 1
        const val DEFAULT_MAX_BATCH_SIZE = 4
    }

    init {
        require(startingBatchSize >= DEFAULT_MIN_BATCH_SIZE) {
            "Starting batch size must be at least $DEFAULT_MIN_BATCH_SIZE"
        }
        require(maxBatchSize >= startingBatchSize) {
            "Max batch size must be >= starting batch size"
        }
    }

    /**
     * Calculate next batch size based on progression type
     */
    fun nextBatchSize(currentSize: Int): Int {
        return when (progressionType) {
            ProgressionType.LINEAR -> NPCDecisions.nextBatchSize(
                currentSize,
                maxBatchSize,
            )

            ProgressionType.FIXED -> currentSize.coerceAtMost(maxBatchSize)
            ProgressionType.RANDOM -> {
                val min = startingBatchSize.coerceAtMost(currentSize)
                val max = (currentSize + 1).coerceAtMost(maxBatchSize)
                (min..max).random()
            }
        }
    }

    /**
     * Check if batching should continue
     */
    fun shouldContinue(currentBatch: Int): Boolean {
        return infiniteBatches || currentBatch < maxBatchSize
    }
}

enum class SpawnMode {
    ON_TARGET_HIT,
    AUTOMATIC,
    MANUAL_COMMAND,
}

enum class ProgressionType {
    LINEAR,
    FIXED,
    RANDOM,
}
