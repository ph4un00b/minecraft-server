package com.colosseum.arena

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
}
