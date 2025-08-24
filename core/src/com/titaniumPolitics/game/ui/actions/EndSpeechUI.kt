package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.stack
import ktx.scene2d.table


class EndSpeechUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("EndSpeechTitle", gameState, actionCallback) {
    private val sbjChar get() = gameState.characters[subject]!!
    private val charSelector = CharacterSelectButton({ nextSpeaker = it })
    var nextSpeaker = ""

    init {
        val st = stack {
            it.grow()
            table {
                add(this@EndSpeechUI.charSelector).size(180f)
                row()
                button {
                    it.fill()
                    label("Submit", "docTitle") {
                        setAlignment(Align.center)
                        color = Color.BLACK
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {

                            this@EndSpeechUI.actionCallback(
                                EndSpeech(
                                    this@EndSpeechUI.subject,
                                    this@EndSpeechUI.sbjChar.place.name
                                ).apply {
                                    nextSpeaker = this@EndSpeechUI.nextSpeaker
                                }
                            )
                            this@EndSpeechUI.onClose.forEach { it() }
                        }
                    })
                }
            }
        }
        content.add(st).grow()


    }

    fun refresh() {
        nextSpeaker = gameState.player.currentMeeting!!.currentCharacters.first { it != subject }
        charSelector.availableCharacters =
            gameState.player.currentMeeting!!.currentCharacters.filter { it != subject }.toSet()
        charSelector.setLabel(nextSpeaker)
    }


}