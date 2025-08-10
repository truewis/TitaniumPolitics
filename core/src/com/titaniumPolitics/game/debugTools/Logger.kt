package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameState

class Logger {

    companion object {
        lateinit var gState: GameState
        fun write(txt: String, level: LogLevel = LogLevel.WARNING) {
            if (level == LogLevel.ACTION_VERBOSE) return
            if (level == LogLevel.APPARATUS_VERBOSE) return
            println("[${gState.time}::${gState.formatTime()}]$level: $txt")
            if (level == LogLevel.ERROR || level == LogLevel.WARNING)
                println("GameState Dumped: ${gState.dump()}")

        }
    }

    enum class LogLevel {
        INFO, WARNING, ERROR, ACTION_VERBOSE, APPARATUS_VERBOSE
    }
}
