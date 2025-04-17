package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.ui.CapsuleStage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.io.File
import kotlin.io.readText

class GameEngineTest {
    lateinit var gState: GameState

    @org.junit.jupiter.api.Test
    fun loadInit() {
        println("Working Directory = " + System.getProperty("user.dir"))
        gState = Json.decodeFromString(
            GameState.serializer(), File("../assets/json/init.json").readText()
        ).also {
            //To run tests, control the player character with an agent.
            it.nonPlayerAgents[it.playerName] = NonPlayerAgent()
            println("Loading complete.")
            it.initialize()
        }
        gState.onStart.forEach { it() }
    }

    @org.junit.jupiter.api.Test
    fun runFor2Days() {
        println("Working Directory = " + System.getProperty("user.dir"))
        gState = Json.decodeFromString(
            GameState.serializer(), File("../assets/json/init.json").readText()
        ).also {
            //To run tests, control the player character with an agent.
            it.nonPlayerAgents[it.playerName] = NonPlayerAgent()
            println("Loading complete.")
            it.initialize()
        }
        gState.onStart.forEach { it() }
        val engine = GameEngine(gState)
        engine.runFor2Days()
    }

    fun GameEngine.runFor2Days() {
        //Start the game.
        println("Game started. Time: ${gameState.time}. Starting main loop.")
        //Main loop
        while (gameState.time < 96) {
            gameLoop()
        }
    }

    @org.junit.jupiter.api.AfterEach
    fun after() {
        gState.dump()
    }


}