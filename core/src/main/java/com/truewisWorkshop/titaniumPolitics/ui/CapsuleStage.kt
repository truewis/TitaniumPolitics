package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.FitViewport
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.io.path.Path

class CapsuleStage(val gameState: GameState) : Stage(FitViewport(1920F, 1080F)) {
    var background = Image()

    //val inputEnabled = ArrayList<(Boolean)->Unit>() Unused
    val logBox = LogUI(gameState)
    var hud: InterfaceRoot
    val rootStack = Stack()
    val assetManager = AssetManager()
    val onMouseClick = ArrayList<(Float, Float) -> Unit>()
    val onMouseDown = ArrayList<(Float, Float) -> Unit>()
    val onKeyDown = ArrayList<(Int) -> Unit>()

    init {
        Logger.write("Initializing CapsuleStage...", Logger.LogLevel.INFO)
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
        assetManager.load("MapGrid.png", Texture::class.java)
        ReadOnly.charJson.forEach {
            val idleImagePath = it.value.jsonObject["image"]?.jsonPrimitive?.content
            println("Loading character images for ${it.key}, idle image path: $idleImagePath")
            // If idleImagePath points to idle.png, load all emotion images in the same directory as well.
            if (idleImagePath != null && idleImagePath.endsWith("idle.png")) {
                val basePath = idleImagePath.removeSuffix("idle.png")
                // emotions to load defined by files present in the directory
                val emotions = listOf(
                    "idle.png",
                    //TODO: Add more emotions and corresponding images.
                    //"happy.png",
                    "smile.png",
                    //"sad.png",
                    "angry.png",
                    "surprised.png",
                    "confused.png",
                    //"determined.png"
                )
                emotions.forEach { emotionImage ->
                    // Check if the file exists before loading
                    val fullPath = basePath + emotionImage
                    val fileHandle = Gdx.files.internal(fullPath)
                    if (fileHandle.exists()) {
                        assetManager.load(fullPath, Texture::class.java)
                        println("Loaded emotion image for ${it.key}: $fullPath")
                    } else {
                        println("Emotion image not found for ${it.key}: $fullPath")
                    }
                }
            } else {
                assetManager.load(
                    it.value.jsonObject["image"]?.jsonPrimitive?.content ?: "portraits/${it.key}.png",
                    Texture::class.java
                )
            }
            assetManager.load(
                it.value.jsonObject["headImage"]?.jsonPrimitive?.content ?: "portraits/${it.key}Head.png",
                Texture::class.java
            )
        }
        ReadOnly.appJson.forEach {
            assetManager.load(it.value.jsonObject["image"]!!.jsonPrimitive.content, Texture::class.java)
        }
        assetManager.finishLoading()
        Logger.write("Explicit asset imports successful.", Logger.LogLevel.INFO)

        rootStack.setFillParent(true)
        rootStack.add(background)
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
        }
        Logger.write("Starting Audio...", Logger.LogLevel.INFO)
        playMusic()
        Logger.write("CapsuleStage initialized successfully.", Logger.LogLevel.INFO)
    }

    fun playMusic() {
        SoundEngine.playMusic("Capsule_old_lighthouse_loop.mp3")
    }

    fun roomChanged(name: String) {
        background.addAction(
            Actions.sequence(
                Actions.fadeOut(0.5f),
                Actions.run {
                    try {
                        background.drawable = TextureRegionDrawable(
                            assetManager.get(
                                ReadOnly.mapJson[if (name.contains("home")) "home" else name]!!.jsonObject["image"]!!.jsonPrimitive.content,
                                Texture::class.java
                            )!!
                        )
                    } catch (e: Exception) {
                        Logger.write("Background Image Error: $e", Logger.LogLevel.INFO)
                    }
                    try {
                        val sound =
                            Gdx.audio.newSound(Gdx.files.internal(ReadOnly.mapJson[if (name.contains("home")) "home" else name]!!.jsonObject["sound"]!!.jsonPrimitive.content))
                        sound.play()//TODO: use SoundEngine.
                    } catch (e: Exception) {
                        Logger.write("Background Sound Error: $e", Logger.LogLevel.INFO)
                    }
                },
                Actions.fadeIn(0.5f)
            )
        )

    }


    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        onMouseClick.forEach { it(screenX.toFloat(), screenY.toFloat()) }
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        onMouseDown.forEach { it(screenX.toFloat(), screenY.toFloat()) }
        return super.touchDown(screenX, screenY, pointer, button)
    }

    override fun keyDown(keyCode: Int): Boolean {
        onKeyDown.forEach { it(keyCode) }
        if (keyCode == Input.Keys.ESCAPE)
            SystemUI.instance.isVisible = !SystemUI.instance.isVisible
        return super.keyDown(keyCode)
    }

    companion object {
        lateinit var instance: CapsuleStage
    }


}
