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
import com.titaniumPolitics.game.core.Party.Role
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.HireManager
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton
import ktx.scene2d.button
import ktx.scene2d.buttonGroup
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
        CharacterSelectButton(action.availableEmployees().toSet()) {
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
    var newHire = action.availableEmployees().first()

    init {

        charSelector.setLabel(newHire)
        val st = stack {
            it.grow()
            table {
                add(this@HireManagerUI.charSelector).size(180f)
                row()
                add(this@HireManagerUI.workplaceSelector)
                row()
                buttonGroup(1, 1) {
                    Role.entries.forEach {
                        val r = it
                        button("toggle") {
                            isChecked = this@HireManagerUI.action.role == r
                            addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent?, actor: Actor?) {
                                    if (isChecked) {
                                        this@HireManagerUI.action.role = r
                                        this@HireManagerUI.submitButton.refresh(this@HireManagerUI.action)
                                    }
                                }
                            })
                        }.inCell.size(150f, 60f).pad(5f)
                    }
                }
                row()
                add(this@HireManagerUI.submitButton)
            }
        }
        content.add(st).grow()


    }


}