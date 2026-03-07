package com.colosseum.npc

import kotlin.random.Random

object NPCDecisions {
    private const val DEFAULT_MAX_NPCS = 4

    fun nextBatchSize(currentSize: Int, maxSize: Int = DEFAULT_MAX_NPCS): Int {
        return (currentSize + 1).coerceAtMost(maxSize)
    }

    fun selectRandomType(): NPCAttackType {
        val roll = Random.nextDouble()
        var cumulative = 0.0
        for (type in NPCAttackType.entries) {
            cumulative += type.probability
            if (roll < cumulative) {
                return type
            }
        }
        return NPCAttackType.SWORD
    }

    fun selectRandomTypesForBatch(batchSize: Int): List<NPCAttackType> {
        return (1..batchSize).map { selectRandomType() }
    }
}
