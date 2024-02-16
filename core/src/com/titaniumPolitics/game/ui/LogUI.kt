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
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.rafaskoberg.gdx.typinglabel.TypingAdapter
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.concurrent.thread

class LogUI(val gameState: GameState) : Table(defaultSkin)
{
    val stk = Stack()

    // Displays current log item.
    val currentTextDisplay = TypingLabel("", skin, "consoleWhite")

    // Displays old log items, up to five lines.
    val oldTextDisplay = Label("", skin, "consoleWhite")
    val ctnuButton = Label(">>>", skin, "console")
    var isPlaying = false
    val donePlayingLine = ArrayList<(Int) -> Unit>()

    //Logs to be played.
    val logQueue = ArrayList<String>()
    val logTimeQueue = ArrayList<Int>()
    val logLineNumberQueue = ArrayList<Int>()
    var currentLineNumber = -1
    var maxDisplayLine = 20

    // Called and cleared when the ctnuButton is pressed.
    var ctnuCallback: () -> Unit = {}
    var isInputEnabled = false
    var playerActionList =
        ArrayList<String>()//Temporary solution for player action. Unnecessary if all the actions has the corresponding UI.
    var numberMode = false
    var numberModeCallback: (Int) -> Unit = {}

    init
    {
        instance = this
        add(stk).grow()
        gameState.log.newItemAdded += { it, time, line ->
            Gdx.app.postRunnable {
                if (!isPlaying)
                {
                    isPlaying = true
                    appendText(it)
                    currentLineNumber = line
                    //(stage as CapsuleStage).setInputEnable("log", false)
                } else
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
        currentTextDisplay.setFontScale(1f)
        oldTextDisplay.setFontScale(1f)
        currentTextDisplay.touchable = Touchable.disabled
        oldTextDisplay.touchable = Touchable.disabled
        currentTextDisplay.typingListener = object : TypingAdapter()
        {
            // Sense TypingLabel animation end and play next log in queue.
            override fun end()
            {
                super.end()
                donePlayingLine.forEach { it(currentLineNumber) }
                if (logQueue.isEmpty())
                {
                    isPlaying = false
                    //(stage as CapsuleStage).setInputEnable("log", true)
                } else
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
        ctnuButton.addListener(object : ClickListener()
        {
            override fun clicked(event: InputEvent, x: Float, y: Float)
            {
                super.clicked(event, x, y)
                ctnuCallback()
                ctnuCallback = {}
                ctnuButton.isVisible = false
            }
        })
        GameEngine.acquireEvent += {
            if (it.type == "Action")
            {
                isInputEnabled = true
            }
        }
        appendText("Welcome to Capsule Zero")
        GameEngine.acquireEvent += {//Print the action list. This is unnecessary if tall the action has the corresponding UI.
            if (it.type == "Action")
            {
                appendText(
                    (it.variables["actionList"] as ArrayList<String>).toString().replace("[", "").replace("]", "")
                )
                playerActionList = it.variables["actionList"] as ArrayList<String>
                isInputEnabled = true
            }
        }
    }

    fun appendText(it: String)
    {
        val oldText = oldTextDisplay.text.split('\n')
        var displayedOldText = ""
        oldText.forEachIndexed { i, x ->
            if (i >= oldText.size - maxDisplayLine)
            {
                displayedOldText += "\n" + x
            }
        }
        displayedOldText += "\n" + currentTextDisplay.text
        oldTextDisplay.setText("[#006600FF]" + displayedOldText)

        currentTextDisplay.restart(it)
    }

    override fun act(delta: Float)
    {
        super.act(delta)
        if (isInputEnabled)
        {
            var choice = -1
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))
            {
                choice = 0
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))
            {
                choice = 1
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3))
            {
                choice = 2
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4))
            {
                choice = 3
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5))
            {
                choice = 4
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6))
            {
                choice = 5
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7))
            {
                choice = 6
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8))
            {
                choice = 7
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9))
            {
                choice = 8
            }
            if (choice != -1)
            {
                if (numberMode) numberModeCallback(choice)
                else
                    when (playerActionList[choice])
                    {
                        "trade" ->
                        {
                            TradeUI.instance.open()
                        }

                        "command" ->
                        {
                            (stage as CapsuleStage).commandBox.open()
                        }

                        else ->
                        {
                            val action =
                                Class.forName("com.titaniumPolitics.game.core.gameActions." + playerActionList[choice])
                                    .getDeclaredConstructor(String::class.java, String::class.java).newInstance(
                                        gameState.playerAgent,
                                        gameState.places.values.find { it.characters.contains(gameState.playerAgent) }!!.name
                                    ) as GameAction
                            action.injectParent(gameState)
                            thread(start = true) {
                                action.chooseParams()
                                GameEngine.acquireCallback(action)
                            }
                        }
                    }
                isInputEnabled = false
            }
        }
    }

    companion object
    {
        lateinit var instance: LogUI
    }
}