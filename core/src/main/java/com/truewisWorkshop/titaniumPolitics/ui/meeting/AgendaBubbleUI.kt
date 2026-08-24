package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.MeterUI
import ktx.scene2d.KTable
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.stack

class AgendaBubbleUI(val agenda: MeetingAgenda) : Table(), KTable {

    init {
        addListener(AgendaTooltipUI(agenda))

        with(agenda) {
            val persuasivenessMin = ReadOnly.const("AgendaPersuasivenessMin")
            val persuasivenessMax = ReadOnly.const("AgendaPersuasivenessMax")
            val persuasivenessFill =
                ((persuasiveness - persuasivenessMin) / (persuasivenessMax - persuasivenessMin)).toFloat().coerceIn(0f, 1f)
            val persuasivenessBarColor = when {
                persuasiveness < 0 -> Color(0.9f, 0.25f, 0.25f, 1f)
                persuasiveness >= persuasivenessMax -> Color(0.25f, 0.85f, 0.3f, 1f)
                else -> Color(0.95f, 0.8f, 0.2f, 1f)
            }

            stack {
                it.size(80f, 80f).fill()
                image("BubbleShade") {

                }
                when (type) {


                    AgendaType.PROOF_OF_WORK -> {
                        image("icon_app_147")
                    }

                    AgendaType.NOMINATE -> {
                        image("icon_app_8")
                    }

                    AgendaType.REQUEST -> {
                        image("icon_gesture_58")
                    }

                    AgendaType.PROMISE -> {
                        image("icon_gesture_58")
                    }

                    AgendaType.PRAISE -> {
                        image("icon_gesture_1")
                    }

                    AgendaType.DENOUNCE -> {
                        image("icon_gesture_2")
                    }

                    AgendaType.PRAISE_PARTY -> {
                        image("icon_gesture_1")
                    }

                    AgendaType.DENOUNCE_PARTY -> {
                        image("icon_gesture_2")
                    }

                    AgendaType.BUDGET_PROPOSAL -> {
                        image("icon_app_104")
                    }

                    AgendaType.BUDGET_RESOLUTION -> {
                        image("icon_app_105")
                    }

                    AgendaType.APPOINT_MEETING -> {
                        image("icon_app_18")
                    }

                    AgendaType.FIRE_MANAGER -> {
                        image("icon_app_7")
                    }

                    else -> {

                    }
                }
                container(stack {
                    container(MeterUI().apply {
                        setValue(persuasivenessFill)
                        color = persuasivenessBarColor
                    }) {
                        fill()
                    }
                    label(this@AgendaBubbleUI.agenda.persuasiveness.toInt().toString(), "docTitle") {
                        setFontScale(0.16f)
                        setAlignment(Align.center)
                    }
                }) {
                    size(58f, 16f)
                    padBottom(4f)
                    align(Align.bottom)
                }
            }
        }
    }

}