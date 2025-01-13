package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import ktx.scene2d.KTable
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.stack

class AgendaBubbleUI(val agenda: MeetingAgenda) : Table(), KTable
{

    init
    {
        addListener(AgendaTooltipUI(agenda))

        with(agenda) {
            stack {
                it.size(80f, 80f).fill()
                image("BadgeRound") {

                }
                when (type)
                {


                    AgendaType.PROOF_OF_WORK -> label("PoW", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.NOMINATE -> label("Nom", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.REQUEST -> label("Req", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.PRAISE -> label("Pr", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.DENOUNCE -> label("De", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.PRAISE_PARTY -> label("PP", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.DENOUNCE_PARTY -> label("DP", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.BUDGET_PROPOSAL -> label("BP", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.BUDGET_RESOLUTION -> label("BR", "console") {
                        setFontScale(2f)
                    }

                    AgendaType.APPOINT_MEETING -> label("AM", "console") {
                        setFontScale(2f)
                    }

                    else ->
                    {

                    }
                }
            }
        }
    }

}