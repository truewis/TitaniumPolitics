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
        val infoSelectPane = ScrollPane(dataTable)
        infoSelectPane.setScrollingDisabled(true, false)

        val st = stack {
            it.grow()
            table {
                add(this@AddInfoUI.agendaTable)
                row()
                add(infoSelectPane)
                row()
                add(this@AddInfoUI.submitButton)
            }
        }
        content.add(st).grow()


    }

    fun refresh() {
        agenda = sbjChar.currentMeeting!!.currentAgenda ?: return
        agendaTable.apply {
            clear()
            button("check") {
                isChecked = true
                isDisabled = true
                add(AgendaBubbleUI(this@AddInfoUI.agenda))
            }
        }
        refreshInfoOptions()
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
                    ).first != 0.0

        }.keys
        dataTable.clear()
        dataTable.apply {
            add(buttonGroup(1, 1) {
                availableInfoKeys.forEach { key ->
                    button("check") {
                        it.size(300f, 100f)
                        label(this@AddInfoUI.gameState.informations[key]!!.simpleDescription(), "docTitle") {
                            it.size(300f, 50f)
                            setAlignment(Align.center)
                            color = Color.WHITE
                            setFontScale(0.2f)
                            wrap = true
                        }
                        row()
                        val eff = this@AddInfoUI.agenda.effectivity(
                            this@AddInfoUI.gameState,
                            meeting = this@AddInfoUI.sbjChar.currentMeeting!!,
                            info = this@AddInfoUI.gameState.informations[key]!!,
                            sbjCharObj = this@AddInfoUI.sbjChar
                        ).first
                        label("%.1f %%".format(eff), "docTitle") {
                            it.size(300f, 50f)
                            setAlignment(Align.center)
                            color = Color.WHITE
                            setFontScale(0.2f)
                        }
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