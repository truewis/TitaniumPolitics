package com.titaniumPolitics.game.ui

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.Scene2DSkin

class CapsuleStage(val gameState: GameState) : Stage(FitViewport(1920F, 1080F))
{
    var background = Image()

    //val inputEnabled = ArrayList<(Boolean)->Unit>() Unused
    val logBox = LogUI(gameState)
    var hud: HeadUpInterface
    val rootStack = Stack()
    val charactersView = CharacterPortraitsUI(gameState)
    val meeting = MeetingUI(gameState)
    val assetManager = AssetManager()
    val commandBox: CommandUI
    val onMouseClick = ArrayList<(Float, Float) -> Unit>()
    val onMouseDown = ArrayList<(Float, Float) -> Unit>()

    init
    {
        println("Initializing CapsuleStage...")
        instance = this
        val resolver = InternalFileHandleResolver()
        assetManager.setLoader(
            Texture::class.java, TextureLoader(resolver)
        )

        ReadOnly.mapJson.forEach {
            assetManager.load(it.value.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)
            assetManager.load("data/dev/capsuleDevBoxCheck.png", Texture::class.java)
            assetManager.load("data/dev/capsuleDevBox.png", Texture::class.java)

        }
        println("Explicit asset imports successful.")
        assetManager.finishLoading()

        rootStack.setFillParent(true)
        rootStack.add(background)
        rootStack.add(charactersView)
        rootStack.add(meeting)
        background.setFillParent(true)

        addActor(rootStack)
        addActor(logBox)
        logBox.setFillParent(true)
        logBox.isVisible = false
        hud = HeadUpInterface(gameState)
        addActor(hud)
        hud.setFillParent(true)

        commandBox = CommandUI(Scene2DSkin.defaultSkin, gameState)
        addActor(commandBox)
        commandBox.setFillParent(true)

        gameState.updateUI.add {
            roomChanged(it.player.place.name)
            if (it.player.currentMeeting != null)
            {
                meeting.isVisible = true
                charactersView.isVisible = false
            } else
            {
                meeting.isVisible = false
                charactersView.isVisible = true
            }
        }
        println("CapsuleStage initialized successfully.")
    }

    fun roomChanged(name: String)
    {
        try
        {

            background.drawable = TextureRegionDrawable(
                assetManager.get(
                    ReadOnly.mapJson[if (name.contains("home")) "home" else name]!!.jsonObject["image"]!!.jsonPrimitive.content,
                    Texture::class.java
                )!!
            )

        } catch (e: Exception)
        {
            println("Background Image Error: $e")
        }
    }

    override fun keyTyped(character: Char): Boolean
    {

        return super.keyTyped(character)
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
    {
        onMouseClick.forEach { it(screenX.toFloat(), screenY.toFloat()) }
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean
    {
        onMouseDown.forEach { it(screenX.toFloat(), screenY.toFloat()) }
        return super.touchDown(screenX, screenY, pointer, button)
    }

    companion object
    {
        lateinit var instance: CapsuleStage
    }


}