package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.EntryClass
import com.titaniumPolitics.game.GameEngineThreadHandler
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.textField
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class QuickLoad() : Table(defaultSkin), KTable {
    val path = textField { }

    init {
        button {
            label("Load", "docTitle")
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    val savedGamePath = this@QuickLoad.path.text
                    //TODO: Check MainMenu for the same logic.
                    Logger.write("Loading saved game from $savedGamePath...", Logger.LogLevel.INFO)
                    val newGame = Json.decodeFromString(
                        GameState.serializer(),
                        Gdx.files.internal(savedGamePath).readString()
                    ).also {
                        it.injectDependency()
                        Logger.write("Loading complete.", Logger.LogLevel.INFO)
                        EntryClass.instance.stage = CapsuleStage(it)
                        Gdx.input.inputProcessor = EntryClass.instance.stage
                    }

                    Logger.write("Starting game engine.", Logger.LogLevel.INFO)

                    GameEngineThreadHandler.startEngine(newGame)
                }
            })
        }
        add(path).size(500f, 100f)
        path.text = "saveBeforeMeeting.json" // Default path
    }


}
