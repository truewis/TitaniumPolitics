package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.rafaskoberg.gdx.typinglabel.TypingAdapter
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import ktx.scene2d.Scene2DSkin.defaultSkin

class LocationUI (val gameState: GameState) : Table(defaultSkin) {
    val stk = Stack()
    // Displays current log item.
    val currentTextDisplay = TypingLabel("", skin, "consoleWhite")
    // Displays old log items, up to five lines.
    val oldTextDisplay = Label("", skin, "consoleWhite")
    val ctnuButton = Label(">>>",skin, "console")
    var isPlaying = false
    val donePlayingLine = ArrayList<(Int)->Unit>()
    //Logs to be played.
    val logQueue = ArrayList<String>()
    val logTimeQueue = ArrayList<Int>()
    val logLineNumberQueue = ArrayList<Int>()
    var currentLineNumber  = -1
    var maxDisplayLine = 40
    // Called and cleared when the ctnuButton is pressed.
    var ctnuCallback : ()->Unit = {}
    var isInputEnabled = false
    init {
        add(stk).grow()
        gameState.log.newItemAdded+={it, time, line->
            Gdx.app.postRunnable {
                if(!isPlaying)
                {
                    isPlaying = true
                    appendText(it)
                    currentLineNumber = line
                    //(stage as CapsuleStage).setInputEnable("log", false)
                }
                else
                {
                    logQueue.add(it)
                    logTimeQueue.add(time)
                    logLineNumberQueue.add(line)
                }
            }
        }
        row()
        val t = Table(skin)
        stk.add(t)
        t.add(oldTextDisplay).grow()
        t.row()
        t.add(currentTextDisplay).growX().fillY()
        oldTextDisplay.setAlignment(Align.bottomLeft)
        currentTextDisplay.setFontScale(2f)
        oldTextDisplay.setFontScale(2f)
        currentTextDisplay.touchable = Touchable.disabled
        oldTextDisplay.touchable = Touchable.disabled
        currentTextDisplay.typingListener = object : TypingAdapter() {// Sense TypingLabel animation end and play next log in queue.
        override fun end() {
            super.end()
            donePlayingLine.forEach { it(currentLineNumber) }
            if (logQueue.isEmpty())
            {
                isPlaying = false
                //(stage as CapsuleStage).setInputEnable("log", true)
            }
            else
            {
                appendText(logQueue.removeFirst())
                logTimeQueue.removeFirst()
                currentLineNumber = logLineNumberQueue.removeFirst()
            }

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
        GameEngine.acquireEvent+={
            if(it.type=="Action")
            {
                isInputEnabled = true
            }
        }
        appendText("Welcome to Capsule Zero")
    }
    fun appendText(it: String){
        val oldText = oldTextDisplay.text.split('\n')
        var displayedOldText = ""
        oldText.forEachIndexed{ i, x->
            if(i>=oldText.size-maxDisplayLine) {
                displayedOldText += "\n"+x
            }
        }
        displayedOldText += "\n"+currentTextDisplay.text
        oldTextDisplay.setText("[#006600FF]"+displayedOldText)

        currentTextDisplay.setText("[#006600FF]"+it)
        currentTextDisplay.restart()
    }

    override fun act(delta: Float) {
        super.act(delta)
        if(isInputEnabled) {
            var choice = -1
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                choice = 0
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                choice = 1
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                choice = 2
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                choice = 3
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
                choice = 4
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
                choice = 5
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7)) {
                choice = 6
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8)) {
                choice = 7
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9)) {
                choice = 8
            }
            if(choice!=-1) {
                GameEngine.acquireCallback(choice)
                isInputEnabled = false
            }
        }
    }
}