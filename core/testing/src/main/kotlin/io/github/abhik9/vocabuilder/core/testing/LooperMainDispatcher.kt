package io.github.abhik9.vocabuilder.core.testing

import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * Android's main dispatcher on the JVM: one "main" thread working through a queue, and an [immediate]
 * variant that runs coroutines in place when already on that thread.
 *
 * Unlike the test dispatchers, it sends what `yield()` hands it to the back of the queue, as Android
 * does. That's what lets updates to different flows reach a `combine()` out of order, so tests can
 * catch UI state built from such flows. It runs in real time: wait for results with timeouts.
 */
class LooperMainDispatcher private constructor(
    private val looper: Looper,
    private val isImmediate: Boolean,
) : MainCoroutineDispatcher(),
    AutoCloseable {
    constructor() : this(Looper(), isImmediate = false)

    override val immediate: MainCoroutineDispatcher =
        if (isImmediate) this else LooperMainDispatcher(looper, isImmediate = true)

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !isImmediate || !looper.isCurrentThread

    override fun dispatch(context: CoroutineContext, block: Runnable) = looper.post(block)

    /** Stops the main thread. Work sent after that is dropped, as by a looper that quit. */
    override fun close() = looper.quit()

    override fun toString() = if (isImmediate) "Main.immediate" else "Main"

    private class Looper {
        @Volatile
        private var thread: Thread? = null
        private val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "main").also { thread = it }
        }

        val isCurrentThread: Boolean get() = Thread.currentThread() === thread

        fun post(block: Runnable) {
            try {
                executor.execute(block)
            } catch (e: RejectedExecutionException) {
                // Quit.
            }
        }

        fun quit() = executor.shutdown()
    }
}
