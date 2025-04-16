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
import com.titaniumPolitics.game.core.GameState
import com.rafaskoberg.gdx.typinglabel.TypingAdapter
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.collections.ArrayList

class DialogueUI(val gameState: GameState) : Table(defaultSkin), KTable {
    var currentDialogue = ""
    var currentDialogueLength = 0
    var currentLineNumber = 0

    // Displays current dialogue line.
    val currentTextDisplay = TypingLabel("", skin, "consoleWhite")

    val speakerNameDisplay = Label("", skin, "consoleWhite")
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
        speakerNameDisplay.setAlignment(Align.bottomLeft)
        currentTextDisplay.setFontScale(3f)
        speakerNameDisplay.setFontScale(4f)
        currentTextDisplay.touchable = Touchable.disabled
        speakerNameDisplay.touchable = Touchable.disabled
        currentTextDisplay.typingListener = object : TypingAdapter() {
            // Sense TypingLabel animation end and play next log in queue.
            override fun end() {
                super.end()

            }
        }
        currentTextDisplay.wrap = true
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

    fun nextLine() {
        if (currentLineNumber < currentDialogueLength - 1) {
            currentLineNumber++
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
        background.drawable = TextureRegionDrawable(
            (stage as CapsuleStage).assetManager.get(
                ReadOnly.mapJson[if (placeName.contains("home")) "home" else placeName]!!.jsonObject["image"]!!.jsonPrimitive.content,
                Texture::class.java
            )!!
        )
        currentDialogue = dialogueKey
        currentDialogueLength = Gdx.files.internal("texts/$currentDialogue.txt").readString().split("\n").size
        currentLineNumber = 0
        playLine(currentLineNumber)
    }

    fun playLine(lineNumber: Int) {
        if (lineNumber > currentDialogueLength) {
            println("Warning: Dialogue line number out of range in $currentDialogue.")
            return
        }
        val line = Gdx.files.internal("texts/$currentDialogue.txt").readString().split("\n")[lineNumber]
        val lineSpeaker = line.split(": ")[0]
        val lineText =
            line.split(": ")[1]
        speakerNameDisplay.setText(ReadOnly.prop(lineSpeaker))
        currentTextDisplay.restart(lineText)
        portraitsTable.clear()
        var prefwidth = 0f
        portraitsTable.add(
            SimplePortraitUI(lineSpeaker, gameState, 1f)
        ).expand().align(Align.bottom).prefHeight(800f).prefWidth(prefwidth)
    }


    companion object {
        lateinit var instance: DialogueUI
    }
}