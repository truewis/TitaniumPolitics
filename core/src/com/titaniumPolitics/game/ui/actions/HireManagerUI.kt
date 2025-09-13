package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.HireManager
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.selectBox
import ktx.scene2d.stack
import ktx.scene2d.table


class HireManagerUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("EndSpeechTitle", gameState, actionCallback) {
    private val sbjChar get() = gameState.characters[subject]!!
    private val action = HireManager(
        this@HireManagerUI.subject,
        this@HireManagerUI.sbjChar.place.name,
        this@HireManagerUI.gameState
    )
    private val charSelector =
        CharacterSelectButton(gameState.player.currentMeeting!!.currentCharacters.filter { it != subject }
            .toSet()) {
            newHire = it
            submitButton.refresh(
                action.apply {
                    newHire = this@HireManagerUI.newHire
                })
        }
    private val workplaceSelector = PlaceSelectButton({
        submitButton.refresh(
            action.apply {
                newHire = this@HireManagerUI.newHire
            })
    })
    var newHire = gameState.player.currentMeeting!!.currentCharacters.first { it != subject }

    init {

        charSelector.setLabel(newHire)
        val st = stack {
            it.grow()
            table {
                add(this@HireManagerUI.charSelector).size(180f)
                row()
//                selectBox<String> {
//                    items = Array(this@NewAgendaUI.praisableParty.toTypedArray())
//                    addListener(object : ChangeListener() {
//                        override fun changed(event: ChangeEvent?, actor: Actor?) {
//                            this@NewAgendaUI.agenda =
//                                MeetingAgenda(
//                                    AgendaType.PRAISE_PARTY,
//                                    this@NewAgendaUI.subject,
//                                    hashMapOf("party" to selected)
//                                )
//                        }
//                    })
//                }.inCell.size(300f, 70f)
                row()
                add(this@HireManagerUI.submitButton)
            }
        }
        content.add(st).grow()


    }


}