package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameState

class Logger
{

    companion object
    {
        lateinit var gState: GameState
        fun warning(txt: String)
        {
            println("Warning: $txt")
            println("GameState Dumped: ${gState.dump()}")

        }
    }
}
