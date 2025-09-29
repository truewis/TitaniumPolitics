package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.GameEngine.Companion.AcquireParams
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Sleep
import com.titaniumPolitics.game.core.gameActions.StartMeeting
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.AlertUI
import com.titaniumPolitics.game.ui.ProgressBackgroundUI
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import ktx.scene2d.*

enum class WaitUIMode {
    WAIT, SLEEP
}

class WaitUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("EndSpeechTitle", gameState, actionCallback) {
    private val sbjChar = gameState.characters[subject]!!
    var interrupted = false
    var mode = WaitUIMode.WAIT // Default mode is WAIT
    var amount = 5
    private val timeSelector = scene2d.buttonGroup(1, 1) {
    }

    init {
        gameState.onAddInfo += this::waitInterruptCondition
        val st = stack {
            it.grow()
            table {
                add(this@WaitUI.timeSelector).size(150f)
                row()
                button("document") {
                    it.fill()
                    label("Submit", "docTitle") {
                        setAlignment(Align.center)
                        color = Color.BLACK
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@WaitUI.interrupted = false
                            GameEngine.acquireEvent += this@WaitUI::spendTime
                            this@WaitUI.spendTime(AcquireParams("", hashMapOf()))
                            if (this@WaitUI.mode == WaitUIMode.SLEEP) {
                                ProgressBackgroundUI.Companion.instance.setVisibleWithFade(true, "Sleep")
                            } else {
                                ProgressBackgroundUI.Companion.instance.setVisibleWithFade(true, "Wait")
                            }

                            this@WaitUI.onClose.forEach { it() }
                        }
                    })
                }
            }
        }
        content.add(st).grow()


    }

    /**
     * This function is called every time the player turn starts while the player is waiting
     * It is called last time when the wait amount is exhausted or the wait is interrupted, but it does not add a new wait action in that case.
     */
    fun spendTime(AcquireParams: GameEngine.Companion.AcquireParams) {
        if (interrupted) {
            GameEngine.acquireEvent -= this::spendTime
            gameState.onAddInfo -= this::waitInterruptCondition
            ProgressBackgroundUI.Companion.instance.setVisibleWithFade(
                false,
                if (mode == WaitUIMode.WAIT) "Wait" else "Sleep"
            )
            return
        }
        if (amount <= 0) {
            GameEngine.acquireEvent -= this::spendTime
            gameState.onAddInfo -= this::waitInterruptCondition
            ProgressBackgroundUI.Companion.instance.setVisibleWithFade(
                false,
                if (mode == WaitUIMode.WAIT) "Wait" else "Sleep"
            )
            return
        }
        if (mode == WaitUIMode.SLEEP) {
            amount -= ReadOnly.constInt("SleepDuration")
            this.actionCallback(
                Sleep(
                    this.subject,
                    this.sbjChar.place.name,
                    gameState
                )
            )
        } else {
            amount -= ReadOnly.constInt("WaitDuration")
            this.actionCallback(
                Wait(
                    this.subject,
                    this.sbjChar.place.name,
                    gameState
                )
            )
        }

    }

    fun refresh(mode: WaitUIMode) {
        this.mode = mode
        timeSelector.clear()
        val list = arrayListOf<Int>()
        if (mode == WaitUIMode.SLEEP) {
            this.titleLabel.setText(ReadOnly.prop("SleepTitle"))
            list.addAll(listOf(60, 120, 180, 240, 300, 360, 420, 480))
            amount = 60 // Default to 1 hour
        } else {
            this.titleLabel.setText(ReadOnly.prop("WaitTitle"))
            list.addAll(listOf(5, 15, 30, 60, 120, 240))
            amount = 5 // Default to 5 minutes
        }
        for (i in list) {
            with(timeSelector) {
                button("document") {
                    label(if (i < 60) "${i}m" else "${i / 60}h", "docTitle") {
                        color = Color.BLACK
                    }
                    it.fill()
                    it.size(100f)
                    isChecked = i == this@WaitUI.amount // Default to 5 minutes
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@WaitUI.amount = i
                        }
                    }
                    )

                }
            }

        }
    }

    private fun waitInterruptCondition(info: Information) {
        if (interrupted)
            return // If already interrupted, do not process further.

        // If the meeting has just started, interrupt once so that the player can see the meeting.
        if (info.tgtPlace == gameState.player.place.name && info.tgtCharacter != gameState.playerName &&
            info.knownTo.contains(gameState.playerName) && info.action is StartMeeting
        ) {
            AlertUI.Companion.instance.addAlert("interrupted", ReadOnly.prop(info.tgtCharacter ?: "Someone"))
            interrupted = true
            Logger.write("WaitUI: Wait interrupted by ${info.author} at ${info.tgtPlace}", Logger.LogLevel.INFO)
        }

        if (gameState.player.currentMeeting != null) {
            // If the player is in a meeting, do not interrupt.
            return
        }
        //Interrupt if a character performs an action other than wait or move in this place.
        if (info.tgtPlace == gameState.player.place.name && info.tgtCharacter != gameState.playerName && info.tgtCharacter in gameState.knownCharactersToPlayer &&
            info.action !is Wait && info.action !is Move && info.knownTo.contains(gameState.playerName)
        ) {

            AlertUI.Companion.instance.addAlert("interrupted", ReadOnly.prop(info.tgtCharacter ?: "Someone"))
            interrupted = true
            Logger.write("WaitUI: Wait interrupted by ${info.author} at ${info.tgtPlace}", Logger.LogLevel.INFO)
        }

    }

    //Override this method instead of remove, remove is not called properly.
    override fun setParent(parent: Group?) {
        super.setParent(parent)
    }

}