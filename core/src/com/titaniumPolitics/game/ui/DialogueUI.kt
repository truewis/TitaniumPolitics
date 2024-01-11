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
import ktx.scene2d.Scene2DSkin.defaultSkin

class DialogueUI (val gameState: GameState) : Table(defaultSkin) {
    val stk = Stack()
    // Displays current log item.
    val currentTextDisplay = TypingLabel("", skin, "consoleWhite")
    // Displays old log items, up to five lines.
    val speakerNameDisplay = Label("", skin, "consoleWhite")
    val ctnuButton = Label(">>>",skin, "console")
    var isPlaying = false
    val donePlayingLine = ArrayList<(Int)->Unit>()
    //Logs to be played.
    // Called and cleared when the ctnuButton is pressed.
    var ctnuCallback : ()->Unit = {}

    init {
        instance = this
        add(stk).grow()
        row()
        val t = Table(skin)
        stk.add(t)
        t.add(speakerNameDisplay).fill()
        t.row()
        t.add(currentTextDisplay).grow()
        speakerNameDisplay.setAlignment(Align.bottomLeft)
        currentTextDisplay.setFontScale(1f)
        speakerNameDisplay.setFontScale(1f)
        currentTextDisplay.touchable = Touchable.disabled
        speakerNameDisplay.touchable = Touchable.disabled
        currentTextDisplay.typingListener = object : TypingAdapter() {// Sense TypingLabel animation end and play next log in queue.
        override fun end() {
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
        addActor(ctnuButton)
        ctnuButton.isVisible = false
        ctnuButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                ctnuCallback()
                ctnuCallback = {}
                ctnuButton.isVisible = false
            }
        })
    }
    fun playDialogue(it: String){

        currentTextDisplay.restart(it)
    }

    companion object {
        lateinit var instance: DialogueUI
    }
}