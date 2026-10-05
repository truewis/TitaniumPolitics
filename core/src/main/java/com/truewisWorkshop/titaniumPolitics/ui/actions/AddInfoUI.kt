package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.AgendaEffectivityEvaluation
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.meeting.AgendaBubbleUI
import com.titaniumPolitics.game.ui.widget.AddInfoEffectivityTooltipUI
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

    private fun allowUnpreparedInfo(): Boolean =
        sbjChar.currentMeeting?.type == Meeting.MeetingType.TALK

    fun refreshInfoOptions() {
        val meeting = sbjChar.currentMeeting!!
        val evaluations = hashMapOf<String, AgendaEffectivityEvaluation>()
        val availableInfoKeys = gameState.informations.filter { (key, info) ->
            val isPrepared = key in gameState.player.preparedInfoKeys
            val isTalkUnpreparedAllowed = allowUnpreparedInfo()
            val matchesKnownInfo = gameState.playerName in info.knownTo
            val notPresented = !meeting.agendas.flatMap { it.informationKeys }
                .contains(key) // Not presented in the current meeting
            if (!(isPrepared || isTalkUnpreparedAllowed) || !matchesKnownInfo || !notPresented) {
                false
            } else {
                val evaluation = agenda.effectivityEvaluation(gameState, meeting, info, sbjChar)
                if (evaluation.effectivity == 0.0) {
                    false
                } else {
                    evaluations[key] = evaluation
                    true
                }
            }

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
                        val evaluation = evaluations[key]!!
                        val reasons = this@AddInfoUI.gameState.getSignificantEffectivityReasons(
                            this@AddInfoUI.agenda,
                            meeting,
                            this@AddInfoUI.gameState.informations[key]!!,
                            this@AddInfoUI.sbjChar,
                            evaluation
                        )
                        val eff = evaluation.effectivity
                        label("%.1f %%".format(eff), "docTitle") {
                            it.size(300f, 50f)
                            setAlignment(Align.center)
                            color = Color.WHITE
                            setFontScale(0.2f)
                        }
                        this@button.addListener(AddInfoEffectivityTooltipUI(evaluation, reasons))
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