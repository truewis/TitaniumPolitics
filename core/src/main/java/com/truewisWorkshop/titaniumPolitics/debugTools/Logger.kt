package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameState
import java.io.BufferedWriter
import java.io.File
import kotlin.io.bufferedWriter

class Logger {

    companion object {
        data class CapturedWrite(val text: String, val level: LogLevel)
        data class CapturedResult<T>(val result: Result<T>, val writes: List<CapturedWrite>)

        lateinit var writer: BufferedWriter
        lateinit var writer1: BufferedWriter
        lateinit var writer2: BufferedWriter
        lateinit var writer3: BufferedWriter
        lateinit var gState: GameState
        private var loggingInitialized = false
        private val capturedWrites = ThreadLocal<MutableList<CapturedWrite>?>()

        fun <T> captureWrites(block: () -> T): CapturedResult<T> {
            val writes = arrayListOf<CapturedWrite>()
            val previous = capturedWrites.get()
            capturedWrites.set(writes)
            val result = try {
                Result.success(block())
            } catch (error: Throwable) {
                Result.failure(error)
            } finally {
                if (previous == null) capturedWrites.remove() else capturedWrites.set(previous)
            }
            return CapturedResult(result, writes)
        }

        fun flushCapturedWrites(writes: List<CapturedWrite>) {
            writes.forEach { write(it.text, it.level) }
        }

        fun format(txt: String, level: LogLevel): String {
            return "[${gState.time}::${gState.formatTime()}]$level: $txt"
        }

        fun init() {
            if (loggingInitialized) close()
            File(gState.workingDirectory).mkdirs()
            writer = File(gState.workingDirectory + "/log.txt").bufferedWriter()
            writer1 = File(gState.workingDirectory + "/actions.txt").bufferedWriter()
            writer2 = File(gState.workingDirectory + "/apparatus.txt").bufferedWriter()
            writer3 = File(gState.workingDirectory + "/conditions.txt").bufferedWriter()
            loggingInitialized = true
            write("Logger initialized.", LogLevel.INFO)
            writer.flush()
        }

        fun write(txt: String, level: LogLevel = LogLevel.WARNING) {
            capturedWrites.get()?.let {
                it += CapturedWrite(txt, level)
                return
            }
            if (!loggingInitialized || !this::gState.isInitialized) {
                println("$level: $txt")
                return
            }

            when (level) {
                LogLevel.INFO -> {
                    writer.write(format(txt, level) + '\n')
                    println(format(txt, level))
                }

                LogLevel.WARNING -> {
                    writer.write(format(txt, level) + '\n')
                    println(format(txt, level))
                    println("GameState Dumped: ${gState.dump()}")
                }

                LogLevel.ERROR -> {
                    writer.write(format(txt, level) + '\n')
                    println(format(txt, level))
                    println("GameState Dumped: ${gState.dump()}")
                }

                LogLevel.ACTION_VERBOSE -> writer1.write(format(txt, level) + '\n')
                LogLevel.APPARATUS_VERBOSE -> writer2.write(format(txt, level) + '\n')
                LogLevel.CONDITION_VERBOSE -> writer3.write(format(txt, level) + '\n')
            }

        }

        fun close() {
            if (!loggingInitialized) return
            writer.flush()
            writer.close()
            writer1.flush()
            writer1.close()
            writer2.flush()
            writer2.close()
            writer3.flush()
            writer3.close()
            loggingInitialized = false
        }
    }

    enum class LogLevel {
        INFO, WARNING, ERROR, ACTION_VERBOSE, APPARATUS_VERBOSE, CONDITION_VERBOSE
    }
}
