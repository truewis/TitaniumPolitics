package com.titaniumPolitics.game

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.SkinLoader
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.I18NBundle
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.MainMenu
import ktx.scene2d.Scene2DSkin
import java.util.Locale

class EntryClass @JvmOverloads constructor(
    private val fastGuiFonts: Boolean = false,
    private val profileStartup: Boolean = false
) : ApplicationAdapter() {
    lateinit var stage: Stage
    lateinit var skin: Skin
    private val assetManager = AssetManager()
    private var startupStarted = 0L
    private var firstGameFrameLogged = false

    fun loadFonts(): ObjectMap<String, Any> {
        if (fastGuiFonts) {
            val fontMap = ObjectMap<String, Any>()
            fastFontSizes.forEach { (key, size) ->
                val font = BitmapFont()
                font.data.setScale(size / font.lineHeight)
                fontMap.put(key, font)
            }
            return fontMap
        }

        val fontMap = ObjectMap<String, Any>()
        val targetLocale = Locale("ko") // TODO: Change this to dynamically load the current language locale


        // Load the I18N bundle (which reads the correct properties file: fonts.properties or fonts_ko.properties)
        val fontBundle = I18NBundle.createBundle(Gdx.files.internal("fonts/fonts"), targetLocale)

        // 1. Define the original font map keys and the target locale.
        // These keys must match the prefixes in the properties files (e.g., 'fixedsys').
        val fontKeys = fontBundle.get("fontKeys").split(",").map { it.trim() }

        // Load the necessary character set once.
        // This ensures Unicode support for the generated fonts.
        val characterSet = Gdx.files.internal("fonts/korean2350.txt").readString("UTF-8")
        //fontBundle.get("charset")
        // --- Font Generation Loop ---
        for (key in fontKeys) {
            // Retrieve localized settings from the loaded bundle
            val fontFile = fontBundle.get("$key.file") // e.g., "Fonts/NanumGothic-Regular.ttf"
            val fontSizeString = fontBundle.get("$key.size")
            val fontSize = fontSizeString.toInt()

            Gdx.app.log("FONT_LOADER", "Loading font $key using file: $fontFile at size: $fontSize")

            val gen = FreeTypeFontGenerator(Gdx.files.internal(fontFile))
            val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()

            // Set parameters from properties file and enable Unicode
            parameter.size = fontSize
            //No need to set character set every time if incremental is true
            //parameter.characters = characterSet
            parameter.incremental = true
            //parameter.minFilter = Texture.TextureFilter.Nearest;
            //parameter.magFilter = Texture.TextureFilter.MipMapLinearNearest;
            //gen.scaleForPixelHeight(fontSize)
            val localizedFont = gen.generateFont(parameter)
            //localizedFont.setUseIntegerPositions(false) // Better rendering for small fonts
            FreeTypeFontGenerator.setMaxTextureSize(4096) //Larger Maximum Texture Size Than Default(1024), especially for east asian fonts. CRITICAL.
            fontMap.put(key, localizedFont)

            //gen.dispose() // Dispose generator to avoid memory leaks, only when incremental is false
        }
        return fontMap
    }

    override fun create() {
        startupStarted = System.nanoTime()
        val fonts = loadFonts()
        logStartupPhase("Fonts", startupStarted)
        val skinStarted = System.nanoTime()
        val param = SkinLoader.SkinParameter(fonts)
        assetManager.load("skin/titaniumSkin.skin", Skin::class.java, param)
        assetManager.finishLoading()
        skin = assetManager.get("skin/titaniumSkin.skin")
        logStartupPhase("Skin", skinStarted)
        Scene2DSkin.defaultSkin = skin
        val menuStarted = System.nanoTime()
        stage = MainMenu(this)
        logStartupPhase("Main menu", menuStarted)
        Gdx.input.inputProcessor = stage
        instance = this
    }

    fun logStartupPhase(phase: String, started: Long) {
        if (profileStartup) {
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            Gdx.app.log("STARTUP_PROFILE", "$phase: ${elapsedMs}ms")
        }
    }

    override fun render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
        if (profileStartup && !firstGameFrameLogged && stage is CapsuleStage) {
            firstGameFrameLogged = true
            logStartupPhase("First game frame since create", startupStarted)
        }
    }

    override fun dispose() {
        stage.dispose()
        GameEngineThreadHandler.stopEngine()
        assetManager.dispose()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    companion object {
        internal val fastFontSizes = mapOf(
            "fixedsys" to 20f,
            "GrotleyRegular" to 75f,
            "Martius" to 75f,
            "MartiusEnglish" to 75f
        )
        lateinit var instance: EntryClass
    }
}
