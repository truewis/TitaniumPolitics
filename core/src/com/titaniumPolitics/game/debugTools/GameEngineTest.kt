package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.ReadOnly.const
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

class GameEngineTest {
    lateinit var gState: GameState
    val gdh =
        GameDataHandler("data${System.currentTimeMillis()}")

    @Test
    fun runFor2Days() {

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
            Logger.write("Reloading test complete.", Logger.LogLevel.INFO)
        }
        val engine2 = GameEngine(gState)
        engine2.runUntil(4)
    }

    fun GameEngine.runUntil(days: Int) {
        //Start the game.
        Logger.write("Game started. Time: ${gameState.time}. Starting main loop.", Logger.LogLevel.INFO)
        //Main loop
        while (gameState.time < days * const("lengthOfDay")) {
            gameLoop()
            gameState.debug()
            if (gameState.time % 60 == 0)
                gdh.writeEveryTurn(gState)
        }
    }

    @AfterEach
    fun after() {
        gState.dump()
        gdh.close()
    }

    val missedMeetings = hashSetOf<String>()
    fun GameState.debug() {

        scheduledMeetings.filter {
            it.value.time + ReadOnly.constInt("MeetingStartTolerance") < time && !missedMeetings.contains(
                it.key
            )
        }.forEach {
            missedMeetings.add(it.key)
            Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
            Logger.write("!Missed meeting:${it.key} at ${it.value.place}.", Logger.LogLevel.INFO)
            Logger.write("Scheduled: ${GameState.formatTime(it.value.time)}", Logger.LogLevel.INFO)
            Logger.write("What people are doing:", Logger.LogLevel.INFO)
            it.value.scheduledCharacters.forEach { ch ->
                Logger.write(
                    "\t$ch:${characters[ch]!!.place.name}, doing ${characters[ch]!!.history.last()}",
                    Logger.LogLevel.INFO
                )
                if (nonPlayerAgents[ch] is NonPlayerAgent) {
                    Logger.write(
                        "\t\tunder ${(nonPlayerAgents[ch] as NonPlayerAgent).routines[0]::class.java.simpleName}",
                        Logger.LogLevel.INFO
                    )
                    Logger.write(
                        "\t\troutine started: ${
                            GameState.formatTime(
                                (nonPlayerAgents[ch] as NonPlayerAgent
                                        ).routines[0].intVariables["routineStartTime"] ?: 0
                            )
                        }",
                        Logger.LogLevel.INFO
                    )
                }
            }
            Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
        }

        if (time % 60 == 0 && hour == 12)
            if (!characters.filter { it.value.history.last() == "sleep" }.keys.isEmpty()) {
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
                characters.filter { it.value.history.last() == "sleep" }.forEach {
                    Logger.write(
                        "${it.key} is still asleep at noon: health:${it.value.health}, will:${it.value.will}, hunger:${it.value.hunger}, thirst:${it.value.thirst}",
                        Logger.LogLevel.INFO
                    )
                }
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
            }

        if (time % 60 == 0) {
            val suffocating = aliveCharacters.filter { entry ->
                entry.value.place.gasPressure("oxygen") < const("CriticalOxygenPressure") || entry.value.place.gasPressure(
                    "carbonDioxide"
                ) / entry.value.place.gasPressure(
                    "oxygen"
                ) > const("CriticalCarbonDioxideRatio")
            }.keys
            if (!suffocating.isEmpty()) {
                Logger.write("!${suffocating} is suffocating", Logger.LogLevel.INFO)

            }
            val hot = aliveCharacters.filter { entry ->
                entry.value.place.temperature - 300 /*[K]*/ !in -const("TemperatureDifferenceTolerance")..const("TemperatureDifferenceTolerance")
            }.keys

            //If temperature is extreme, take damage.
            if (!hot.isEmpty()
            ) {
//
                Logger.write("!${hot} is under extreme temperature!", Logger.LogLevel.INFO)
            }

        }
    }


}