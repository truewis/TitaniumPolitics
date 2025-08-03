package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.meeting.AgendaBubbleUI
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.WindowUI

import ktx.scene2d.*


class AddInfoUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("AddInfoTitle", gameState, actionCallback) {
    private val dataTable = Table()
    private var targetTable = Table()
    private var agendaTable = scene2d.buttonGroup(1, 1)
    private val sbjChar get() = gameState.characters[subject]!!
    var infoKey = ""
    lateinit var agenda: MeetingAgenda

    init {
        isVisible = false
        val agendaSelectPane = ScrollPane(agendaTable)
        agendaSelectPane.setScrollingDisabled(false, true)

        val infoSelectPane = ScrollPane(dataTable)
        infoSelectPane.setScrollingDisabled(true, false)

        val infoDescPane = ScrollPane(targetTable)
        infoDescPane.setScrollingDisabled(false, true)

        val st = stack {
            it.grow()
            table {
                add(agendaSelectPane)
                row()
                add(infoSelectPane)
                row()
                add(infoDescPane)
                row()
                button {
                    it.fill()
                    label("Submit", "docTitle") {
                        setAlignment(Align.center)
                        color = Color.BLACK
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {

                            this@AddInfoUI.actionCallback(
                                AddInfo(
                                    this@AddInfoUI.subject,
                                    this@AddInfoUI.sbjChar.place.name
                                ).apply {
                                    infoKey = this@AddInfoUI.infoKey
                                    agendaIndex =
                                        this@AddInfoUI.gameState.player.currentMeeting!!.agendas.indexOf(this@AddInfoUI.agenda)
                                }
                            )
                            this@AddInfoUI.onClose.forEach { it() }
                        }
                    })
                }
            }
        }
        content.add(st).grow()


    }

    fun refresh() {
        agenda = sbjChar.currentMeeting!!.agendas.first()
        agendaTable.apply {
            clear()
            this@AddInfoUI.sbjChar.currentMeeting?.agendas?.forEach { agenda ->
                button("check") {
                    isChecked = agenda == this@AddInfoUI.agenda
                    add(AgendaBubbleUI(agenda))
                    addListener(object : ClickListener() {
                        override fun clicked(
                            event: InputEvent?,
                            x: Float,
                            y: Float
                        ) {
                            this@AddInfoUI.agenda = agenda
                        }
                    })
                }
            }
        }

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
                                this@AddInfoUI.targetTable.clear()
                                this@AddInfoUI.targetTable.add(
                                    scene2d.label(
                                        this@AddInfoUI.gameState.informations[key]!!.simpleDescription(),
                                        "docTitle"
                                    ) {
                                        color = Color.BLACK
                                        setAlignment(Align.center)
                                        wrap = true
                                    }).grow()
                            }
                        })
                    }
                }
            })
        }
    }


}