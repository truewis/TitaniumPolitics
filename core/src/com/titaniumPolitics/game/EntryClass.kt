package com.titaniumPolitics.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.titaniumPolitics.game.ui.MainMenu
import kotlinx.serialization.json.Json
import ktx.scene2d.Scene2DSkin
import kotlin.concurrent.thread

class EntryClass : ApplicationAdapter()
{
    lateinit var stage: Stage
    lateinit var skin: Skin
    override fun create()
    {

        val gen = FreeTypeFontGenerator(Gdx.files.internal("Fonts/LondrinaSolid-Regular.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 14
        //Include the below line for Unicode support
        //parameter.characters = Gdx.files.internal("korean2350.txt").readString("UTF-8")
        val nanum = gen.generateFont(parameter)
        fontMap.put("fixedsys", nanum)
        gen.dispose()
        val param = SkinLoader.SkinParameter(fontMap)
        val assetManager = AssetManager()
        assetManager.load("skin/titaniumSkin.json", Skin::class.java, param)
        assetManager.finishLoading()
        skin = assetManager.get("skin/titaniumSkin.json")
        Scene2DSkin.defaultSkin = skin
        stage = MainMenu(this)
        Gdx.input.inputProcessor = stage
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
