package com.titaniumPolitics.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.CapsuleStage
import kotlinx.serialization.json.Json
import ktx.scene2d.Scene2DSkin
import kotlin.concurrent.thread

class EntryClass : ApplicationAdapter()
{
    lateinit var stage: CapsuleStage
    lateinit var newGame: GameState
    lateinit var skin: Skin
    override fun create()
    {

        val gen = FreeTypeFontGenerator(Gdx.files.internal("data/DungGeunMo.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 16
        parameter.characters = Gdx.files.internal("korean2350.txt").readString("UTF-8")
        val nanum = gen.generateFont(parameter)
        fontMap.put("fixedsys", nanum)
        gen.dispose()
        val param = SkinLoader.SkinParameter(fontMap)
        val assetManager = AssetManager()
        assetManager.load("skin/capsuleSkin.json", Skin::class.java, param)
        assetManager.finishLoading()
        skin = assetManager.get("skin/capsuleSkin.json")
        Scene2DSkin.defaultSkin = skin
        val savedGamePath = System.getenv("SAVED_GAME")
        if (savedGamePath == null)
        {
            println("Loading init.json...")
            newGame = Json.decodeFromString(
                GameState.serializer(),
                Gdx.files.internal("json/init.json").readString()
            ).also {
                println("Loading complete.")
                stage = CapsuleStage(it)
                Gdx.input.inputProcessor = stage
                it.initialize()
            }
            newGame.onStart.forEach { it() }
        } else
        {
            println("Loading saved game from $savedGamePath...")
            newGame = Json.decodeFromString(
                GameState.serializer(),
                Gdx.files.internal(savedGamePath).readString()
            ).also {
                println("Loading complete.")
                stage = CapsuleStage(it)
                Gdx.input.inputProcessor = stage
                it.initialize()
            }
        }
        println("Starting game engine.")

        thread(start = true) {
            val engine = GameEngine(newGame)
            engine.startGame()
        }
    }

    override fun render()
    {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun dispose()
    {
        stage.dispose()
    }

    override fun resize(width: Int, height: Int)
    {
        stage.viewport.update(width, height, true)
    }

    companion object
    {

        //--------------------------------------------------------------
        var fontMap = ObjectMap<String, Any>()
    }
}
