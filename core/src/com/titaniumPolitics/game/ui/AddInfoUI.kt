package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.meeting.AgendaBubbleUI

import ktx.scene2d.*


class AddInfoUI(val gameState: GameState, override var actionCallback: (GameAction) -> Unit) :
    WindowUI("AddInfoTitle"), ActionUI {
    private val dataTable = Table()
    private var targetTable = Table()
    private var agendaTable = scene2d.buttonGroup(1, 1)

    private var subject = gameState.playerName
    private val sbjChar = gameState.characters[subject]!!
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
                                    agendaIndex =
                                        this@AddInfoUI.gameState.player.currentMeeting!!.agendas.indexOf(this@AddInfoUI.agenda)
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