package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.GameDataHandler
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.NonPlayerAgent
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Calendar
import kotlin.time.Duration.Companion.hours

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
        while (gameState.time < days * const("lengthOfDay"))
        {
            gameLoop()
            gameState.debug()
            if (gameState.time % 60 == 0)
                gdh.writeEveryTurn(gState)
        }
    }

    @AfterEach
    fun after()
    {
        gState.dump()
        gdh.close()
    }

    val missedMeetings = hashSetOf<String>()
    fun GameState.debug()
    {

        scheduledMeetings.filter {
            it.value.time + ReadOnly.constInt("MeetingStartTolerance") < time && !missedMeetings.contains(
                it.key
            )
        }.forEach {
            missedMeetings.add(it.key)
            println("////////////////////////////////////////////////")
            println("!Missed meeting:${it.key} at ${it.value.place}.")
            println("Scheduled: ${GameState.formatTime(it.value.time)}")
            println("What people are doing:")
            it.value.scheduledCharacters.forEach { ch ->
                println("\t$ch:${characters[ch]!!.place.name}, doing ${characters[ch]!!.history.last()}")
                if (nonPlayerAgents[ch] is NonPlayerAgent)
                {
                    println("\t\tunder ${(nonPlayerAgents[ch] as NonPlayerAgent).routines[0]::class.java.simpleName}")
                    println("\t\troutine started: ${GameState.formatTime((nonPlayerAgents[ch] as NonPlayerAgent).routines[0].intVariables["routineStartTime"] ?: 0)}")
                }
            }
            println("////////////////////////////////////////////////")
        }

        if (time % 60 == 0 && hour == 12)
            if (!characters.filter { it.value.history.last() == "sleep" }.keys.isEmpty())
            {
                println("////////////////////////////////////////////////")
                characters.filter { it.value.history.last() == "sleep" }.forEach {
                    println("${it.key} is still asleep at noon: health:${it.value.health}, will:${it.value.will}, hunger:${it.value.hunger}, thirst:${it.value.thirst}")
                }
                println("////////////////////////////////////////////////")
            }

        if (time % 60 == 0)
        {
            val suffocating = characters.filter { entry ->
                entry.value.place.gasPressure("oxygen") < const("CriticalOxygenPressure") || entry.value.place.gasPressure(
                    "carbonDioxide"
                ) / entry.value.place.gasPressure(
                    "oxygen"
                ) > const("CriticalCarbonDioxideRatio")
            }.keys
            if (!suffocating.isEmpty())
            {
                println("!${suffocating} is suffocating")

            }
            val hot = characters.filter { entry ->
                entry.value.place.temperature - 300 /*[K]*/ !in -const("TemperatureDifferenceTolerance")..const("TemperatureDifferenceTolerance")
            }.keys

            //If temperature is extreme, take damage.
            if (!hot.isEmpty()
            )
            {
//
                println("!${hot} is under extreme temperature!")
            }

        }
    }


}