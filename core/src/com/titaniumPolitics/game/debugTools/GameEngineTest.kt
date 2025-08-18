package com.titaniumPolitics.game.debugTools

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.ReadOnly.const
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GameEngineTest {
    lateinit var gState: GameState
    val directory = "data" + Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")
    val gdh =
        GameDataHandler(directory)

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    @Test
    fun runFor2Days() {

        gdh.initializeColumns()
        println("Working Directory = " + System.getProperty("user.dir"))
        gState = Json.Default.decodeFromString(
            GameState.serializer(), File("../assets/json/init.json").readText()
        ).also {
            //To run tests, control the player character with an agent.
            it.workingDirectory = directory
            it.nonPlayerAgents[it.playerName] = NonPlayerAgent()
            println("Loading complete.")
            it.initialize()
        }
        val fNameInit = "$directory/dataInit.json"
        gState.dump(fNameInit)
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
        engine2.runUntilElection()
    }

    fun GameEngine.runUntilElection() {
        //Start the game.
        Logger.write("Game started. Time: ${gameState.time}. Starting main loop.", Logger.LogLevel.INFO)
        //Main loop
        while (gameState.ongoingMeetings.none { it.value.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION }) {
            gameLoop()
            gameState.debug()
            if (gameState.time % 60 == 0)
                gdh.writeEveryTurn(gState)
            if (gameState.time % 1440 == 0)
                gState.dump(directory + "/data" + gState.time + ".json")
        }
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
            if (gameState.time % 1440 == 0)
                gState.dump(directory + "/data" + gState.time + ".json")
        }
    }

    @AfterEach
    fun after() {
        gState.dump(directory + "/data" + gState.time + ".json")
        gdh.close()
    }

    val missedMeetings = hashSetOf<String>()
    fun GameState.debug() {
        characters.forEach {
            it.value.health += 10 //For testing purposes, increase health of all characters by 10.
        }

        //If there are characters with zero will, print their names.
        val zeroWillCharacters = characters.filter { it.value.will <= 0.0 }
        if (zeroWillCharacters.isNotEmpty()) {
//            Logger.write(
//                "Characters with zero will: ${zeroWillCharacters.keys.joinToString(", ")}",
//                Logger.LogLevel.INFO
//            )
        }

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
                    "\t$ch:${characters[ch]!!.history.last { it.startsWith("Action") }}",
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
                                        ).routines[0].routineStartTime
                            )
                        }",
                        Logger.LogLevel.INFO
                    )
                    (nonPlayerAgents[ch] as NonPlayerAgent).routines[0].variables.forEach { (key, value) ->
                        Logger.write("\t\t$key: $value", Logger.LogLevel.INFO)
                    }
                    (nonPlayerAgents[ch] as NonPlayerAgent).routines[0].intVariables.forEach { (key, value) ->
                        Logger.write("\t\t$key: $value", Logger.LogLevel.INFO)
                    }
                }
            }
            Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
        }

        if (time % 60 == 0 && hour == 12)
            if (!characters.filter {
                    it.value.history.last { it.startsWith("Action") }.split(":")[0] == "sleep"
                }.keys.isEmpty()) {
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
                characters.filter { it.value.history.last { it.startsWith("Action") }.split(":")[0] == "sleep" }
                    .forEach {
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
                Logger.write("!suffocating!:${suffocating}", Logger.LogLevel.CONDITION_VERBOSE)

            }
            val hot = aliveCharacters.filter { entry ->
                entry.value.place.temperature - 300 /*[K]*/ !in -const("TemperatureDifferenceTolerance")..const("TemperatureDifferenceTolerance")
            }.keys

            //If temperature is extreme, take damage.
            if (!hot.isEmpty()
            ) {
//
                Logger.write("!under extreme temperature!:${hot}", Logger.LogLevel.CONDITION_VERBOSE)
            }

            val places = suffocating.map { gState.characters[it]!!.place.name }.toSet() +
                    hot.map { gState.characters[it]!!.place.name }.toSet()
            if (places.isNotEmpty()) {
                Logger.write(
                    "Characters in places with extreme conditions: ${places.joinToString(", ")}",
                    Logger.LogLevel.CONDITION_VERBOSE
                )
            }

        }
    }


}