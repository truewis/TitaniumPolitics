package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameState

class Logger {

    companion object {
        lateinit var gState: GameState
        fun write(txt: String, level: LogLevel = LogLevel.WARNING) {
            println("[${gState.formatTime()}]$level: $txt")
            if (level == LogLevel.ERROR || level == LogLevel.WARNING)
                println("GameState Dumped: ${gState.dump()}")

        }
    }

    enum class LogLevel {
        INFO, WARNING, ERROR
    }
}
