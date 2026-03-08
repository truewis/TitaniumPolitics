package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.BudgetDisplayUI
import ktx.scene2d.*

class AgendaTooltipUI(agenda: MeetingAgenda) : Tooltip<Table>(scene2d.table {
    addActor(scene2d.image("TooltipShadow10p") {
        it.width = 450f
        it.height = 450f
        it.x = -50f
        it.y = -50f
        setColor(0f, 0f, 0f, 0.7f)
        touchable = Touchable.disabled//This is a shadow outside the tooltip
    })
    stack {

        it.size(350f)
        image("BlackPx")

        image("NoiseBackground") {
            setColor(1f, 1f, 1f, 0.1f)
        }
        image("PanelDottedShade700x700") {
            setColor(0f, 0f, 0f, 1f)
        }
        table {
            stack {
                it.size(350f, 50f)
                image("TooltipTitle")
                table {
                    label(agenda.type.toString(), "docTitle") {
                        it.growX()
                        setFontScale(0.2f)
                    }
                }
            }
            row()
            if (agenda.attachedRequest != null) {
                val req = agenda.attachedRequest!!
                table {
                    it.size(350f, 300f)
                    defaults().left().pad(2f)
                    label(ReadOnly.prop("AgendaTooltipUI-action"), "docTitle") {
                        setFontScale(0.2f)
                    }
                    label(ReadOnly.prop(req.action::class.simpleName ?: ""), "docTitle") {
                        setFontScale(0.2f)
                        it.growX()
                    }
                    row()
                    label(ReadOnly.prop("AgendaTooltipUI-character"), "docTitle") {
                        setFontScale(0.2f)
                    }
                    label(ReadOnly.charProp(req.action.sbjCharacter), "docTitle") {
                        setFontScale(0.2f)
                        it.growX()
                    }
                    row()
                    label(ReadOnly.prop("AgendaTooltipUI-place"), "docTitle") {
                        setFontScale(0.2f)
                    }
                    label(ReadOnly.placeProp(req.action.tgtPlace), "docTitle") {
                        setFontScale(0.2f)
                        it.growX()
                    }
                    row()
                    label(ReadOnly.prop("AgendaTooltipUI-issuedBy"), "docTitle") {
                        setFontScale(0.2f)
                    }
                    label(req.issuedBy.joinToString { ReadOnly.charProp(it) }, "docTitle") {
                        setFontScale(0.2f)
                        wrap = true
                        it.growX()
                    }
                    row()
                    label(ReadOnly.prop("AgendaTooltipUI-issuedTo"), "docTitle") {
                        setFontScale(0.2f)
                    }
                    label(req.issuedTo.joinToString { ReadOnly.charProp(it) }, "docTitle") {
                        setFontScale(0.2f)
                        wrap = true
                        it.growX()
                    }
                }
            } else if (agenda.attachedBudget != null) {
                table {
                    it.size(350f, 300f)
                    label(
                        ReadOnly.prop(
                            if (agenda.type == AgendaType.BUDGET_RESOLUTION)
                                "NewAgendaUI-budgetResolution-desc"
                            else
                                "NewAgendaUI-budgetProposal-desc"
                        ), "docTitle"
                    ) {
                        setFontScale(0.2f)
                        wrap = true
                        it.growX()
                    }
                    row()
                    add(BudgetDisplayUI(budget = agenda.attachedBudget)).size(300f, 250f)
                }
            } else {
                label(agenda.subjectParams.toString()) {
                    it.size(350f, 150f)
                    setFontScale(2f)
                    setAlignment(Align.topLeft)
                    wrap = true
                }
            }
        }
    }

}) {
    init {
        manager.initialTime = 0.5f
    }

}