package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameDataHandler
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.NonPlayerAgent
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Calendar

class GameEngineTest
{
    lateinit var gState: GameState
    val gdh =
        GameDataHandler("data${System.currentTimeMillis()}")

    @Test
    fun runFor2Days()
    {

        gdh.initializeColumns()
        println("Working Directory = " + System.getProperty("user.dir"))
        gState = Json.Default.decodeFromString(
            GameState.serializer(), File("../assets/json/init.json").readText()
        ).also {
            //To run tests, control the player character with an agent.
            it.nonPlayerAgents[it.playerName] = NonPlayerAgent()
            println("Loading complete.")
            it.initialize()
        }
        gState.onStart.forEach { it() }
        val engine = GameEngine(gState)
        engine.runUntil(2)

        val fName = gState.dump()
        gState = Json.decodeFromString(
            GameState.serializer(),
            File(fName).readText()
        ).also {
            it.injectDependency()
            println("Reloading test complete.")
        }
        val engine2 = GameEngine(gState)
        engine2.runUntil(4)
    }

    fun GameEngine.runUntil(days: Int)
    {
        //Start the game.
        println("Game started. Time: ${gameState.time}. Starting main loop.")
        //Main loop
        while (gameState.time < days * ReadOnly.const("lengthOfDay"))
        {
            gameLoop()
            if (gameState.time % 240 == 0)
                gdh.writeEveryTurn(gState)
        }
    }

    @AfterEach
    fun after()
    {
        gState.dump()
        gdh.close()
    }


}