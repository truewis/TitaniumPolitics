package com.capsulezero.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import kotlinx.serialization.json.Json
import ktx.scene2d.Scene2DSkin
import kotlin.concurrent.thread

class EntryClass : ApplicationAdapter() {
    var stage: CapsuleStage? = null
    var newGame: GameState? = null
    lateinit var skin : Skin
    override fun create() {
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

        newGame = Json.decodeFromString(
                GameState.serializer(),
        Gdx.files.internal("json/init.json").readString()
        ).also { it.injectDependency() }
        stage = CapsuleStage(newGame!!)
        Gdx.input.inputProcessor = stage
        thread(start = true){
            val engine = GameEngine(newGame!!)
            engine.startGame()
        }
    }

    override fun render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        stage!!.act(Gdx.graphics.deltaTime)
        stage!!.draw()
    }

    override fun dispose() {
        stage!!.dispose()
    }

    override fun resize(width: Int, height: Int) {
        stage!!.viewport.update(width, height, true)
    }
    companion object {

        //--------------------------------------------------------------
        var fontMap = ObjectMap<String, Any>()
    }
}
