package com.androidoctor

import com.androidoctor.data.shell.FixLog
import com.androidoctor.data.shell.LineKind
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FixLogTest {
    @Test fun lineIdsStayUniqueUnderConcurrency() = runBlocking {
        val log = FixLog()
        log.run("t", listOf("a")) {
            (1..8).map { async(kotlinx.coroutines.Dispatchers.Default) { repeat(200) { log.line(LineKind.CMD, "x") } } }.awaitAll()
        }
        val ids = log.state.value.lines.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun staleRunCannotCloseNewerRun() {
        val log = FixLog()
        val first = log.begin("one", listOf("a"))
        val second = log.begin("two", listOf("a"))
        log.end(first)
        assertTrue(log.state.value.active)
        assertEquals(second, log.state.value.runId)
        log.end(second)
        assertFalse(log.state.value.active)
    }
}
