package com.colosseum.npc

import kotlin.random.Random

object NPCDecisions {
    private const val MAX_NPCS = 4

    fun nextBatchSize(currentSize: Int): Int {
        return (currentSize + 1).coerceAtMost(MAX_NPCS)
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
