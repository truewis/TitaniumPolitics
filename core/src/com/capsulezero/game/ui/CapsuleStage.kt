package com.capsulezero.game.ui

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.ReadOnlyJsons
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CapsuleStage(val gameState: GameState) : Stage(FitViewport(800.0F, 800.0F)) {
    var background = Image()
    //val inputEnabled = ArrayList<(Boolean)->Unit>() Unused
    val logBox = LogUI(gameState)
    var hud : HeadUpInterface
    val rootStack = Stack()
    val assetManager = AssetManager()

    init {
        val resolver = InternalFileHandleResolver()
        assetManager.setLoader(
            Texture::class.java, TextureLoader(resolver))

        ReadOnlyJsons.mapJson.forEach {
        assetManager.load( it.value.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)
        assetManager.load( "data/dev/capsuleDevBoxCheck.png", Texture::class.java)
        assetManager.load( "data/dev/capsuleDevBox.png", Texture::class.java)
        println(it.value.jsonObject["image"]!!.jsonPrimitive.content)
        }
        assetManager.finishLoading()

        rootStack.setFillParent(true)
        rootStack.add(background)
        background.setFillParent(true)
        addActor(logBox)
        logBox.setFillParent(true)
        addActor(rootStack)
        hud = HeadUpInterface(gameState)
        addActor(hud)
        println(hud.stage)
        println(hud.todoBox.stage)
        hud.setFillParent(true)
        gameState.updateUI.add {
            roomChanged(it.places.values.find { it.characters.contains(gameState.playerAgent) }!!.name)
        }

    }
    fun roomChanged(name: String) {
        background.drawable = TextureRegionDrawable(assetManager.get(ReadOnlyJsons.mapJson[name]!!.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)!!)
    }

    override fun keyTyped(character: Char): Boolean {

        return super.keyTyped(character)
    }









}