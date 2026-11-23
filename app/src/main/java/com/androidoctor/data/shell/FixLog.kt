package com.androidoctor.data.shell

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

enum class StepState { PENDING, RUNNING, DONE, FAILED, SKIPPED }
enum class LineKind { CMD, OUT, ERR, NOTE }

data class LogStep(val title: String, val state: StepState = StepState.PENDING, val summary: String? = null)
data class LogLine(val id: Long, val kind: LineKind, val text: String)

data class FixLogState(
    val title: String = "",
    val steps: List<LogStep> = emptyList(),
    val lines: List<LogLine> = emptyList(),
    val active: Boolean = false,
    val runId: Long = 0,
)

/** Marks the coroutine that owns a fix run, so only that run can write to the log. */
class FixRunElement(val runId: Long) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<FixRunElement>
}

/**
 * Live record of what a fix run is doing: named steps plus every command, path and
 * result line as it happens. Fed by the use cases (steps) and [ShellExecutor] (commands),
 * and shown by the progress screen. Lines are only recorded while a run is active.
 */
@Singleton
class FixLog @Inject constructor() {
    private val _state = MutableStateFlow(FixLogState())
    val state: StateFlow<FixLogState> = _state.asStateFlow()
    private val ids = AtomicLong(0)
    private val runs = AtomicLong(0)

    /** Starts a run and returns its id. A newer run takes over the panel from an older one. */
    fun begin(title: String, steps: List<String>): Long {
        val id = runs.incrementAndGet()
        _state.value = FixLogState(title = title, steps = steps.map { LogStep(it) }, active = true, runId = id)
        return id
    }

    /** Runs [block] as one log run. The run is closed even on cancellation. */
    suspend fun <T> run(title: String, steps: List<String>, block: suspend () -> T): T {
        val id = begin(title, steps)
        try {
            return withContext(FixRunElement(id)) { block() }
        } finally {
            end(id)
        }
    }

    /** True when the calling coroutine belongs to the run that currently owns the log. */
    private suspend fun owns(): Boolean {
        val s = _state.value
        return s.active && currentCoroutineContext()[FixRunElement]?.runId == s.runId
    }

    suspend fun start(index: Int) {
        if (owns()) updateStep(index) { it.copy(state = StepState.RUNNING) }
    }

    suspend fun finish(index: Int, state: StepState = StepState.DONE, summary: String? = null) {
        if (owns()) updateStep(index) { it.copy(state = state, summary = summary) }
    }

    suspend fun line(kind: LineKind, text: String) {
        if (!owns()) return
        val id = ids.incrementAndGet() // generated outside update{}: the lambda may be retried
        _state.update { s ->
            if (!s.active) s else s.copy(lines = (s.lines + LogLine(id, kind, text.take(MAX_LINE))).takeLast(MAX_LINES))
        }
    }

    suspend fun result(exitCode: Int, stdout: String, stderr: String) {
        if (exitCode == 0) {
            stdout.lineSequence().firstOrNull { it.isNotBlank() }?.let { line(LineKind.OUT, "  " + it.trim()) }
        } else {
            val err = stderr.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
            line(LineKind.ERR, "  exit $exitCode ${err}".trimEnd())
        }
    }

    /** Closes the run only if [runId] is still the active one. */
    fun end(runId: Long) = _state.update { if (it.runId == runId) it.copy(active = false) else it }

    private fun updateStep(index: Int, f: (LogStep) -> LogStep) {
        _state.update { s ->
            if (index !in s.steps.indices) s
            else s.copy(steps = s.steps.mapIndexed { i, st -> if (i == index) f(st) else st })
        }
    }
}

private const val MAX_LINE = 140
private const val MAX_LINES = 200
