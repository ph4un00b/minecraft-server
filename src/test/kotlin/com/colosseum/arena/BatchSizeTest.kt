package com.colosseum.arena

import com.colosseum.arena.domain.NPCAttackType
import com.colosseum.arena.domain.NPCDecisions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BatchSizeTest {
    @Test
    fun `next batch size from 1 returns 2`() {
        assertEquals(2, NPCDecisions.nextBatchSize(1))
    }

    @Test
    fun `next batch size from 2 returns 3`() {
        assertEquals(3, NPCDecisions.nextBatchSize(2))
    }

    @Test
    fun `next batch size from 3 returns 4`() {
        assertEquals(4, NPCDecisions.nextBatchSize(3))
    }

    @Test
    fun `next batch size from 4 returns 4`() {
        assertEquals(4, NPCDecisions.nextBatchSize(4))
    }

    @Test
    fun `select random type returns valid type`() {
        repeat(100) {
            val type = NPCDecisions.selectRandomType()
            assertEquals(true, type in NPCAttackType.entries)
        }
    }

    @Test
    fun `probabilities sum to 100 percent`() {
        val total = NPCAttackType.entries.sumOf { it.probability }
        assertEquals(1.0, total, 0.001, "Probabilities must sum to 100%")
    }

    @Test
    fun `select random types for batch returns correct size`() {
        val types = NPCDecisions.selectRandomTypesForBatch(4)
        assertEquals(4, types.size)
    }

    @Test
    fun `random selection approximates probability distribution`() {
        val iterations = 10000
        val counts = NPCAttackType.entries.associate { it to 0 }.toMutableMap()

        repeat(iterations) {
            val type = NPCDecisions.selectRandomType()
            counts[type] = counts[type]!! + 1
        }

        println("Probability distribution test ($iterations iterations):")
        counts.forEach { (type, count) ->
            val actual = count.toDouble() / iterations * 100
            val expected = type.probability * 100
            val msg = "  $type: actual=${String.format("%.1f", actual)}%, " +
                "expected=$expected%"
            println(msg)
        }

        val tolerance = 10.0
        counts.forEach { (type, count) ->
            val actual = count.toDouble() / iterations
            val expected = type.probability
            val diff = kotlin.math.abs(actual - expected)
            val msg = "Type $type: expected ~${expected * 100}% " +
                "but got ${actual * 100}%"
            assertEquals(true, diff < tolerance / 100, msg)
        }
    }
}
