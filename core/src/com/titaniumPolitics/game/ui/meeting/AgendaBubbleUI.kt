package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
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
                image("BubbleShade") {

                }
                when (type)
                {


                    AgendaType.PROOF_OF_WORK ->
                    {
                        image("icon_app_147")
                    }
                    AgendaType.NOMINATE ->
                    {
                        image("icon_app_8")
                    }
                    AgendaType.REQUEST ->
                    {
                        image("icon_gesture_58")
                    }
                    AgendaType.PRAISE ->
                    {
                        image("icon_gesture_1")
                    }
                    AgendaType.DENOUNCE ->
                    {
                        image("icon_gesture_2")
                    }
                    AgendaType.PRAISE_PARTY ->
                    {
                        image("icon_gesture_1")
                    }
                    AgendaType.DENOUNCE_PARTY ->
                    {
                        image("icon_gesture_2")
                    }
                    AgendaType.BUDGET_PROPOSAL ->
                    {
                        image("icon_app_104")
                    }
                    AgendaType.BUDGET_RESOLUTION ->
                    {
                        image("icon_app_105")
                    }
                    AgendaType.APPOINT_MEETING ->
                    {
                        image("icon_app_18")
                    }
                    AgendaType.FIRE_MANAGER ->{
                        image("icon_app_7")
                    }
                    else ->
                    {

                    }
                }
            }
        }
    }

}