package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameState

class Logger {

    companion object {
        lateinit var gState: GameState
        fun write(txt: String, level: LogLevel = LogLevel.WARNING) {
            write("[${gState.formatTime(, LogLevel.INFO)}]$level: $txt")
            if (level == LogLevel.ERROR || level == LogLevel.WARNING)
                write("GameState Dumped: ${gState.dump(, LogLevel.INFO)}")

        }
    }

    enum class LogLevel {
        INFO, WARNING, ERROR
    }
}
