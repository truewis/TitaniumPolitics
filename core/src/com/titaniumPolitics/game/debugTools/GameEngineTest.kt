package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.NonPlayerAgent
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

class GameEngineTest
{
    lateinit var gState: GameState


    @Test
    fun runFor2Days()
    {
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
        Logger.gState = gState
        val engine = GameEngine(gState)
        engine.runFor2Days()
    }

    fun GameEngine.runFor2Days()
    {
        //Start the game.
        println("Game started. Time: ${gameState.time}. Starting main loop.")
        //Main loop
        while (gameState.time < 96)
        {
            gameLoop()
        }
    }

    @AfterEach
    fun after()
    {
        gState.dump()
    }


}