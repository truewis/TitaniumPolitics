package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.TextureLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import com.titaniumPolitics.game.EntryClass
import com.titaniumPolitics.game.GameEngineThreadHandler
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.label
import ktx.scene2d.scene2d
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainMenu(val entry: EntryClass) : Stage(FitViewport(1920F, 1080F)) {
    var background = Image()

    val rootStack = Stack()
    val menu = Table()
    val assetManager = AssetManager()
    val music = Gdx.audio.newMusic(Gdx.files.internal("data/music/mainMenu.mp3"))
    val startbutton = scene2d.label("Click to Start", "trnsprtConsole") {
        addAction(
            Actions.forever(
                Actions.sequence(
                    Actions.delay(0.5f),
                    AlphaAction().apply {
                        duration = 0.2f
                        alpha = 0f
                    },
                    AlphaAction().apply {
                        duration = 0.2f
                        alpha = 1f
                    }
                )))
        setFontScale(3f)
        setAlignment(Align.bottomRight, Align.bottomRight)
        addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                startGame()
            }
        })
    }

    init {

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
        rootStack.add(menu)
        background.setFillParent(true)



        menu.add(startbutton).pad(100f).align(Align.bottomRight).grow()

        addActor(rootStack)
        assetManager.load("data/MainMenu.png", Texture::class.java)
        assetManager.finishLoading()
        background.drawable = TextureRegionDrawable(
            assetManager.get(
                "data/MainMenu.png",
                Texture::class.java
            )!!
        )
        playMusic()
        //Temporary fix
        Gdx.app.postRunnable {
            startGame()
        }
    }

    fun playMusic() {

        music.isLooping = true
        music.play()

    }

    override fun keyTyped(character: Char): Boolean {

        return super.keyTyped(character)
    }

    fun startGame() {
        music.stop()
        val savedGamePath = System.getenv("SAVED_GAME")
        var newGame: GameState
        startbutton.setText("Loading...")
        if (savedGamePath == null) {
            println("Loading init.json...")
            newGame = Json.decodeFromString(
                GameState.serializer(),
                Gdx.files.internal("json/init.json").readString()
            ).also {
                println("Loading complete.")
                it.workingDirectory =
                    "data" + Calendar.getInstance().time.toString("YYYYMMdd_HHmmss")//DO not put this statement when loading existing game or in initialize(); it will mess up with GameEngineTest.
                Logger.gState = it
                Logger.init()
                ReadOnly.setLocale(Locale.KOREAN)
                it.initialize()
                entry.stage = CapsuleStage(it)
                Gdx.input.inputProcessor = entry.stage
            }
            newGame.onStart.forEach { it() }
        } else {
            //TODO: Check QuickLoad for the same logic.
            println("Loading saved game from $savedGamePath...")
            newGame = Json.decodeFromString(
                GameState.serializer(),
                Gdx.files.internal(savedGamePath).readString()
            ).also {
                it.injectDependency()
                println("Loading complete.")
                entry.stage = CapsuleStage(it)
                Gdx.input.inputProcessor = entry.stage
            }
        }
        println("Starting game engine.")
        GameEngineThreadHandler.startEngine(newGame)
    }

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }


    companion object {
        lateinit var instance: MainMenu
    }


}
