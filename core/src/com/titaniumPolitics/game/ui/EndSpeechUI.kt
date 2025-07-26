package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.WindowUI

import ktx.scene2d.*


class EndSpeechUI(val gameState: GameState, override var actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("EndSpeechTitle"), ActionUI {
    private var subject = gameState.playerName
    private val sbjChar = gameState.characters[subject]!!
    private val charSelector = CharacterSelectButton(skin, { nextSpeaker = it })
    var nextSpeaker = ""

    init {
        isVisible = false
        val st = stack {
            it.grow()
            table {
                add(this@EndSpeechUI.charSelector).size(150f)
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
        charSelector.availableCharacters = gameState.player.currentMeeting!!.currentCharacters
        charSelector.setCharacter(nextSpeaker)
    }

    override fun changeSubject(charName: String) {
        subject = charName
    }



}