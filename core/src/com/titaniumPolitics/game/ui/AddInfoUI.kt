package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class AddInfoUI(val gameState: GameState, override var actionCallback: (GameAction) -> Unit) :
    WindowUI("AddInfoTitle"), ActionUI {
    private val dataTable = Table()
    private var targetTable = Table()

    private var subject = gameState.playerName
    private val sbjChar = gameState.characters[subject]!!
    var infoKey = ""

    init {
        isVisible = false
        val currentResourcePane = ScrollPane(dataTable)
        currentResourcePane.setScrollingDisabled(true, false)

        val targetResourcePane = ScrollPane(targetTable)
        targetResourcePane.setScrollingDisabled(false, true)

        val st = stack {
            it.grow()
            table {
                add(currentResourcePane)
                add(targetResourcePane)
                row()
                button {
                    it.fill()
                    label("Submit") {
                        setAlignment(Align.center)
                        setFontScale(3f)
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {

                            this@AddInfoUI.actionCallback(
                                AddInfo(
                                    this@AddInfoUI.subject,
                                    this@AddInfoUI.sbjChar.place.name
                                ).apply {
                                    infoKey = this@AddInfoUI.infoKey
                                }
                            )

                            this@AddInfoUI.isVisible = false
                        }
                    })
                }
            }
        }
        content.add(st).grow()


    }

    fun refresh() {

        val availableInfoKeys = sbjChar.preparedInfoKeys.filter { key ->
            !sbjChar.currentMeeting!!.agendas.flatMap { it.informationKeys }
                .contains(key) // Not presented in the current meeting
        }
        infoKey = availableInfoKeys.first()
        dataTable.clear()
        dataTable.apply {
            add(buttonGroup(1, 1) {
                availableInfoKeys.forEach { key ->
                    button("check") {
                        image("TilesGrunge")
                        isChecked = key == this@AddInfoUI.infoKey
                        this@button.addListener(object : ClickListener() {
                            override fun clicked(
                                event: InputEvent?,
                                x: Float,
                                y: Float
                            ) {
                                this@AddInfoUI.targetTable = scene2d.table {
                                    label(this@AddInfoUI.gameState.informations[key]!!.simpleDescription())

                                }
                            }
                        })
                    }
                }
            })
        }
    }

    override fun changeSubject(charName: String) {
        subject = charName
    }

    companion object {
        lateinit var primary: AddInfoUI
    }


}