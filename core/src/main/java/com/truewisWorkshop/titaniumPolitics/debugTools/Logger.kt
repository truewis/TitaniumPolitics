package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameState
import java.io.BufferedWriter
import java.io.File
import kotlin.io.bufferedWriter

class Logger {

    companion object {
        lateinit var writer: BufferedWriter
        lateinit var writer1: BufferedWriter
        lateinit var writer2: BufferedWriter
        lateinit var writer3: BufferedWriter
        lateinit var gState: GameState
        fun format(txt: String, level: LogLevel): String {
            return "[${gState.time}::${gState.formatTime()}]$level: $txt"
        }

        fun init() {
            File(gState.workingDirectory).mkdirs()
            writer = File(gState.workingDirectory + "/log.txt").bufferedWriter()
            writer1 = File(gState.workingDirectory + "/actions.txt").bufferedWriter()
            writer2 = File(gState.workingDirectory + "/apparatus.txt").bufferedWriter()
            writer3 = File(gState.workingDirectory + "/conditions.txt").bufferedWriter()
            write("Logger initialized.", LogLevel.INFO)
            writer.flush()
        }

        fun write(txt: String, level: LogLevel = LogLevel.WARNING) {
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
            writer.flush()
            writer.close()
            writer1.flush()
            writer1.close()
            writer2.flush()
            writer2.close()
            writer3.flush()
            writer3.close()
        }
    }

    enum class LogLevel {
        INFO, WARNING, ERROR, ACTION_VERBOSE, APPARATUS_VERBOSE, CONDITION_VERBOSE
    }
}
