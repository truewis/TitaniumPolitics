package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingAdapter
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.SimplePortraitUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class DialogueUI(val gameState: GameState) : Table(defaultSkin), KTable {
    var currentDialogue = ""
    var currentDialogueLength = 0
    var currentLineNumber = 0

    // Displays current dialogue line.
    val currentTextDisplay = TypingLabel("", skin, "description").apply {
        setFontScale(0.5f)
        touchable = Touchable.disabled
        wrap = true
        typingListener = object : TypingAdapter() {
            // Sense TypingLabel animation end and play next log in queue.
            override fun end() {
                super.end()

            }
        }
    }

    val speakerNameDisplay = Label("", skin, "description").apply {
        setFontScale(0.7f)
        touchable = Touchable.disabled
        setAlignment(Align.bottomLeft)
    }
    val ctnuButton = Label(">>>", skin, "consoleWhite")
    val donePlayingLine = ArrayList<(Int) -> Unit>()
    val background = Image(defaultSkin, "BackgroundNoiseHD")

    //Logs to be played.
    // Called and cleared when the ctnuButton is pressed.
    var ctnuCallback: () -> Unit = {}
    val portraitsTable = Table(defaultSkin)

    init {
        isVisible = false
        instance = this
        EventSystem.onPlayDialogue += {
            playDialogue(it)
        }
        stack {
            it.grow()
            add(this@DialogueUI.background)
            add(this@DialogueUI.portraitsTable)
            table {
                add().grow()
                row()
                stack {
                    it.growX()
                    val t = table {
                        add(this@DialogueUI.speakerNameDisplay).fill().growX()
                        row()
                        add(this@DialogueUI.currentTextDisplay).fill().growX()
                        row()
                        add(this@DialogueUI.ctnuButton).fill()
                    }
//                    image("GradientBottom") {
//                        setSize(t.prefWidth, t.prefHeight)
//                    }
//                    image("BackgroundNoiseHD") {
//                        setSize(t.prefWidth, t.prefHeight)
//                    }
                }
            }
        }
        ctnuButton.setPosition(1800f, 0f)
        ctnuButton.setFontScale(2f)
        // Blinking ctnuButton
        ctnuButton.addAction(
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
        ctnuButton.isVisible = true
        ctnuButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                if (!currentTextDisplay.hasEnded())
                    currentTextDisplay.skipToTheEnd()
                else
                    nextLine()
                super.clicked(event, x, y)
            }
        })
        addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    if (!currentTextDisplay.hasEnded())
                        currentTextDisplay.skipToTheEnd()
                    else
                        nextLine()
                    return true
                }
                return super.keyDown(event, keycode)
            }
        })
    }

    override fun setVisible(visible: Boolean) {
        if (visible) {
            stage.keyboardFocus = this
        }
        super.setVisible(visible)
    }

    private var dialogueLines: List<String> = emptyList()
    private val activePortraits = mutableMapOf<String, SimplePortraitUI>()

    fun nextLine() {
        if (currentLineNumber < dialogueLines.lastIndex) {
            currentLineNumber++
            val line = dialogueLines[currentLineNumber]

            // Handle special commands before playing dialogue line
            if (line.startsWith("ENTER ")) {
                val name = line.removePrefix("ENTER ").trim()
                addPortrait(name)
                nextLine() // Skip to next real dialogue or command
                return
            }
            if (line.startsWith("EXIT ")) {
                val name = line.removePrefix("EXIT ").trim()
                removePortrait(name)
                nextLine()
                return
            }
            if (line.startsWith("BELL")) {
                TODO()
            }
            if (line.startsWith("SLAM")) {
                TODO()
            }

            playLine(currentLineNumber)
        } else {
            ctnuCallback()
            ctnuCallback = {}
            instance.isVisible = false
        }
    }

    fun playDialogue(dialogueKey: String) {
        isVisible = true

        val placeName = gameState.player.place.name
        val imageKey = if (placeName.contains("home")) "home" else placeName
        val imagePath = ReadOnly.mapJson[imageKey]!!
            .jsonObject["image"]!!
            .jsonPrimitive.content

        background.drawable = TextureRegionDrawable(
            (stage as CapsuleStage).assetManager.get(imagePath, Texture::class.java)
        )

        currentDialogue = dialogueKey
        dialogueLines = Gdx.files.internal("texts/$currentDialogue.txt")
            .readString()
            .split("\n")
            .filter { it.isNotBlank() }

        currentDialogueLength = dialogueLines.size
        currentLineNumber = -1 // So first call to nextLine() gets line 0
        activePortraits.clear()
        portraitsTable.clear()

        nextLine()
    }

    fun playLine(lineNumber: Int) {
        if (lineNumber !in dialogueLines.indices) {
            Logger.write(
                "Warning: Dialogue line number $lineNumber out of range in '$currentDialogue'.",
                Logger.LogLevel.INFO
            )
            return
        }

        val line = dialogueLines[lineNumber]
        val parts = line.split(": ", limit = 2)
        if (parts.size < 2) {
            Logger.write(
                "Warning: Malformed dialogue line in '$currentDialogue' at index $lineNumber: '$line'",
                Logger.LogLevel.INFO
            )
            return
        }
        val prefix = parts[0].split(", ", limit = 2)

        val emotion = if (prefix.size < 2) {
            "idle"
        } else prefix[1]
        val speaker = prefix[0]
        val text = parts[1]

        if (speaker == "Narrator")
            speakerNameDisplay.setText("")
        else
            speakerNameDisplay.setText(ReadOnly.prop(speaker))
        currentTextDisplay.restart(text)

        // Bring the current speaker to the foreground
        bringPortraitToFront(speaker, emotion)
    }

    /** Adds a portrait to the scene */
    private fun addPortrait(name: String) {
        if (activePortraits.containsKey(name)) return
        val portrait = SimplePortraitUI(name, 1f, false).apply {
            color.a = 0f // Start transparent, fade in
            addAction(Actions.fadeIn(0.3f))
        }
        activePortraits[name] = portrait
        portraitsTable.addActor(portrait)
        portrait.setPosition(0f, 0f, Align.bottomLeft) // Adjust position as needed
    }

    /** Removes a portrait from the scene */
    private fun removePortrait(name: String) {
        activePortraits[name]?.let { portrait ->
            portrait.addAction(
                Actions.sequence(
                    Actions.fadeOut(0.3f),
                    Actions.removeActor()
                )
            )
            activePortraits.remove(name)
        }
    }

    /** Moves the given portrait to the foreground and pushes others back */
    private fun bringPortraitToFront(speaker: String, emotion: String) {
        activePortraits.forEach { (name, portrait) ->
            if (name == speaker) {
                // Move to center & brighten alpha
                portrait.addAction(
                    Actions.parallel(
                        Actions.moveTo(portraitsTable.width / 2f - portrait.width / 2f, portrait.y, 0.4f),
                        Actions.fadeIn(0.4f)
                    )
                )
                portrait.setEmotion(emotion)
            } else {
                // Move away & dim alpha
                portrait.addAction(
                    Actions.parallel(
                        Actions.moveBy(-50f, 0f, 0.4f), // Example: slide back
                        Actions.alpha(0.5f, 0.4f)
                    )
                )
            }
        }
    }


    companion object {
        lateinit var instance: DialogueUI
    }
}