package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.rafaskoberg.gdx.typinglabel.TypingAdapter
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.stack
import ktx.scene2d.table

class DialogueUI(val gameState: GameState) : Table(defaultSkin), KTable
{
    val stk = Stack()

    // Displays current log item.
    val currentTextDisplay = TypingLabel("", skin, "consoleWhite")

    // Displays old log items, up to five lines.
    val speakerNameDisplay = Label("", skin, "consoleWhite")
    val ctnuButton = Label(">>>", skin, "console")
    var isPlaying = false
    val donePlayingLine = ArrayList<(Int) -> Unit>()

    //Logs to be played.
    // Called and cleared when the ctnuButton is pressed.
    var ctnuCallback: () -> Unit = {}

    init
    {
        isVisible = false
        instance = this
        stack {
            it.grow()
            image("capsuleDevLabel1")
            table {
                add(this@DialogueUI.speakerNameDisplay).fill()
                row()
                add(this@DialogueUI.currentTextDisplay).grow()
                row()
                add(this@DialogueUI.ctnuButton).fill()
            }
        }
        speakerNameDisplay.setAlignment(Align.bottomLeft)
        currentTextDisplay.setFontScale(2f)
        speakerNameDisplay.setFontScale(2f)
        currentTextDisplay.touchable = Touchable.disabled
        speakerNameDisplay.touchable = Touchable.disabled
        currentTextDisplay.typingListener = object : TypingAdapter()
        {
            // Sense TypingLabel animation end and play next log in queue.
            override fun end()
            {
                super.end()

            }
        }
        ctnuButton.setPosition(1800f, 0f)
        ctnuButton.setFontScale(2f)
        // Blinking ctnuButton
        ctnuButton.addAction(
            Actions.forever(Actions.sequence(
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
        ctnuButton.addListener(object : ClickListener()
        {
            override fun clicked(event: InputEvent, x: Float, y: Float)
            {
                super.clicked(event, x, y)
                ctnuCallback()
                ctnuCallback = {}
                instance.isVisible = false
            }
        })
    }

    fun playDialogue(dialogueKey: String)
    {
        isVisible = true
        currentTextDisplay.restart(dialogueKey)
    }

    companion object
    {
        lateinit var instance: DialogueUI
    }
}