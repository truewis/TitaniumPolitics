package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.meeting.AgendaBubbleUI
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import ktx.scene2d.*


class AddInfoUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("AddInfoTitle", gameState, actionCallback) {
    private val dataTable = Table()
    private var targetTable = Table()
    private var agendaTable = scene2d.buttonGroup(1, 1)
    private val sbjChar get() = gameState.characters[subject]!!
    lateinit var agenda: MeetingAgenda

    init {
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
                add(this@AddInfoUI.submitButton)
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
                            this@AddInfoUI.refreshInfoOptions()
                        }
                    })
                }
            }
        }
        dataTable.clear()
    }

    fun refreshInfoOptions() {
        val availableInfoKeys = gameState.informations.filter { (key, info) ->
            gameState.playerName in info.knownTo &&
                    !sbjChar.currentMeeting!!.agendas.flatMap { it.informationKeys }
                        .contains(key) // Not presented in the current meeting
                    &&
                    //has nonzero effectivity with the selected agenda
                    agenda.effectivity(
                        gameState,
                        meeting = sbjChar.currentMeeting!!,
                        info = info,
                        sbjCharObj = sbjChar
                    ) != 0.0

        }.keys
        dataTable.clear()
        dataTable.apply {
            add(buttonGroup(1, 1) {
                availableInfoKeys.forEach { key ->
                    button("check") {
                        it.size(200f, 100f)
                        image("TilesGrunge")
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

                                this@AddInfoUI.submitButton.refresh(
                                    AddInfo(
                                        this@AddInfoUI.subject,
                                        this@AddInfoUI.tgtPlace,
                                        infoKey = key,
                                        agendaIndex =
                                            this@AddInfoUI.gameState.player.currentMeeting!!.agendas.indexOf(this@AddInfoUI.agenda),
                                        this@AddInfoUI.gameState
                                    )
                                )
                            }
                        })
                    }
                    row()
                }
            })
        }
    }


}