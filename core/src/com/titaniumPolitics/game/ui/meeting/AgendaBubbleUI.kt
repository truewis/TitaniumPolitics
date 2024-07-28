package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.MeetingAgenda
import ktx.scene2d.KTable
import ktx.scene2d.image
import ktx.scene2d.stack

class AgendaBubbleUI(val agenda: MeetingAgenda) : Table(), KTable
{

    init
    {

        with(agenda) {
            stack {
                it.size(100f, 100f).fill()
                image("BadgeRound") {

                }
                when (type)
                {


                    else ->
                    {

                    }
                }
            }
        }
    }

}