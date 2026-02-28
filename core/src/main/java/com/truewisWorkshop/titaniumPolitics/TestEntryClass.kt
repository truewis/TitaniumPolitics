package com.titaniumPolitics.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.NonPlayerAgent
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.MainMenu
import kotlinx.serialization.json.Json
import ktx.scene2d.Scene2DSkin
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TestEntryClass : ApplicationAdapter() {

    lateinit var gState: GameState
    val directory = "data" + Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")

    override fun create() {
        runFor2Days()
    }

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun runFor2Days() {
        println("Working Directory = " + System.getProperty("user.dir"))
        gState = Json.Default.decodeFromString(
            GameState.serializer(), File("../assets/json/init.json").readText()
        ).also {
            //To run tests, control the player character with an agent.
            it.workingDirectory = directory
            it.nonPlayerAgents[it.playerName] = NonPlayerAgent()
            println("Loading complete.")
            Logger.gState = it
            Logger.init()
            ReadOnly.setLocale(Locale.KOREAN)
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
            Logger.gState = it
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
            if (gameState.time % 1440 == 0)
                gState.dump(directory + "/data" + gState.time + ".json")
        }
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

        if (time % 60 == 0 && hour == 12)
            if (!characters.filter {
                    it.value.history.last().split(";")[0] == "sleep"
                }.keys.isEmpty()) {
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
                characters.filter { it.value.history.last().split(";")[0] == "sleep" }
                    .forEach {
                        Logger.write(
                            "${it.key} is still asleep at noon: health:${it.value.health}, will:${it.value.will}, hunger:${it.value.hunger}, thirst:${it.value.thirst}",
                            Logger.LogLevel.INFO
                        )
                    }
                Logger.write("////////////////////////////////////////////////", Logger.LogLevel.INFO)
            }

        if (time % 60 == 0) {
            val suffocating = activeCharacters.filter { entry ->
                entry.value.place.gasPressure("oxygen") < const("CriticalOxygenPressure") || entry.value.place.gasPressure(
                    "carbonDioxide"
                ) / entry.value.place.gasPressure(
                    "oxygen"
                ) > const("CriticalCarbonDioxideRatio")
            }.keys
            if (!suffocating.isEmpty()) {
                Logger.write("!suffocating!:${suffocating}", Logger.LogLevel.CONDITION_VERBOSE)

            }
            val hot = activeCharacters.filter { entry ->
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


    override fun dispose() {
        GameEngineThreadHandler.stopEngine()
    }

}
