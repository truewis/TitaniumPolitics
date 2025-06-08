package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameEngine.Companion.AcquireParams
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Sleep
import com.titaniumPolitics.game.core.gameActions.Wait

import ktx.scene2d.*

enum class WaitUIMode {
    WAIT, SLEEP
}

class WaitUI(val gameState: GameState, override var actionCallback: (GameAction) -> Unit) :
    WindowUI("EndSpeechTitle"), ActionUI {
    private var subject = gameState.playerName
    private val sbjChar = gameState.characters[subject]!!
    var interrupted = false
    var mode = WaitUIMode.WAIT // Default mode is WAIT
    var amount = 5
    private val timeSelector = scene2d.buttonGroup(1, 1) {
    }

    init {
        isVisible = false
        gameState.onAddInfo += this::waitInterruptCondition
        val st = stack {
            it.grow()
            table {
                add(this@WaitUI.timeSelector).size(150f)
                row()
                button {
                    it.fill()
                    label("Submit") {
                        setAlignment(Align.center)
                        setFontScale(3f)
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@WaitUI.interrupted = false
                            GameEngine.acquireEvent += this@WaitUI::spendTime
                            this@WaitUI.spendTime(AcquireParams("", hashMapOf()))
                        }
                    })
                }
            }
        }
        content.add(st).grow()


    }

    fun spendTime(AcquireParams: GameEngine.Companion.AcquireParams) {
        this.actionCallback = GameEngine.acquireCallback
        if (interrupted) {
            this.isVisible = false
            AlertUI.instance.addAlert("interrupted")
            GameEngine.acquireEvent -= this::spendTime
            return
        }
        if (amount <= 0) {
            this.isVisible = false
            GameEngine.acquireEvent -= this::spendTime
            return
        }
        if (mode == WaitUIMode.SLEEP) {
            amount -= ReadOnly.constInt("SleepDuration")
            this.actionCallback(
                Sleep(
                    this.subject,
                    this.sbjChar.place.name
                )
            )
        } else {
            amount -= ReadOnly.constInt("WaitDuration")
            this.actionCallback(
                Wait(
                    this.subject,
                    this.sbjChar.place.name
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
                button {
                    label(if (i < 60) "${i}m" else "${i / 60}h") {
                        setFontScale(2f)
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
        //Interrupt if a character performs an action other than wait in this place.
        if (info.tgtPlace == gameState.player.place.name && info.author != gameState.playerName &&
            !(info.type == InformationType.ACTION && info.action is Wait)
        ) {
            interrupted = true
            println("WaitUI: Wait interrupted by ${info.author} at ${info.tgtPlace}")
        }

    }


    override fun changeSubject(charName: String) {
        subject = charName
    }

    companion object {
        lateinit var primary: WaitUI
    }


}