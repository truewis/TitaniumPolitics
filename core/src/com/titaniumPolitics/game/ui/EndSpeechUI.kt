package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.EndSpeech
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class EndSpeechUI(val gameState: GameState, override var actionCallback: (GameAction) -> Unit) :
    WindowUI("EndSpeechTitle"), ActionUI {
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
                    label("Submit") {
                        setAlignment(Align.center)
                        setFontScale(3f)
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
                            this@EndSpeechUI.isVisible = false
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

    companion object {
        lateinit var primary: EndSpeechUI
    }


}