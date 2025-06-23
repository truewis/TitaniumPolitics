package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
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
import com.titaniumPolitics.game.ui.meeting.MeetingUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CapsuleStage(val gameState: GameState) : Stage(FitViewport(1920F, 1080F)) {
    var background = Image()

    //val inputEnabled = ArrayList<(Boolean)->Unit>() Unused
    val logBox = LogUI(gameState)
    var hud: InterfaceRoot
    val rootStack = Stack()
    val charactersView = CharacterPortraitsUI(gameState)
    val meetingUI = MeetingUI(gameState)
    val assetManager = AssetManager()
    val onMouseClick = ArrayList<(Float, Float) -> Unit>()
    val onMouseDown = ArrayList<(Float, Float) -> Unit>()

    init {
        println("Initializing CapsuleStage...")
        instance = this
        val resolver = InternalFileHandleResolver()
        assetManager.setLoader(
            Texture::class.java, TextureLoader(resolver)
        )

        ReadOnly.mapJson.forEach {
            assetManager.load(it.value.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)
        }
        assetManager.load("data/dev/capsuleDevBoxCheck.png", Texture::class.java)
        assetManager.load("data/dev/capsuleDevBox.png", Texture::class.java)
        assetManager.load("document_small_contrast.png", Texture::class.java)
        assetManager.load("idcard_contrast.png", Texture::class.java)
        ReadOnly.charJson.forEach {
            assetManager.load(it.value.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)
        }
        ReadOnly.appJson.forEach {
            assetManager.load(it.value.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)
        }
        println("Explicit asset imports successful.")
        assetManager.finishLoading()

        rootStack.setFillParent(true)
        rootStack.add(background)
        rootStack.add(charactersView)
        rootStack.add(meetingUI)
        background.setFillParent(true)

        addActor(rootStack)
        addActor(logBox)
        logBox.setFillParent(true)
        logBox.isVisible = false
        hud = InterfaceRoot(gameState)
        addActor(hud)
        hud.setFillParent(true)

        var prevPlace = ""
        gameState.updateUI.add {
            if (prevPlace != it.player.place.name) {
                prevPlace = it.player.place.name
                roomChanged(it.player.place.name)
            }
            if (it.player.currentMeeting != null) {
                meetingUI.isVisible = true
                meetingUI.newMeeting(it.player.currentMeeting!!)
                charactersView.isVisible = false
            } else {
                meetingUI.isVisible = false
                charactersView.isVisible = true
            }
        }
        println("Starting Audio...")
        playMusic()
        println("CapsuleStage initialized successfully.")
    }

    fun playMusic() {
        //val music = Gdx.audio.newMusic(Gdx.files.internal("data/Capsule_old_lighthouse_loop.mp3"))
        val music = Gdx.audio.newMusic(Gdx.files.internal("data/TheAlters1.mp3"))
        music.isLooping = true
        music.play()
    }

    fun roomChanged(name: String) {
        try {

            background.drawable = TextureRegionDrawable(
                assetManager.get(
                    ReadOnly.mapJson[if (name.contains("home")) "home" else name]!!.jsonObject["image"]!!.jsonPrimitive.content,
                    Texture::class.java
                )!!
            )

        } catch (e: Exception) {
            println("Background Image Error: $e")
        }
        try {
            val sound =
                Gdx.audio.newSound(Gdx.files.internal(ReadOnly.mapJson[if (name.contains("home")) "home" else name]!!.jsonObject["sound"]!!.jsonPrimitive.content))
            sound.play()
        } catch (e: Exception) {
            println("Background Sound Error: $e")
        }
    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        onMouseClick.forEach { it(screenX.toFloat(), screenY.toFloat()) }
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        onMouseDown.forEach { it(screenX.toFloat(), screenY.toFloat()) }
        return super.touchDown(screenX, screenY, pointer, button)
    }

    companion object {
        lateinit var instance: CapsuleStage
    }


}