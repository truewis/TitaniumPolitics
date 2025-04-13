package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import ktx.scene2d.KTable
import ktx.scene2d.image
import ktx.scene2d.stack

class InfoBubbleUI(val info: Information) : Table(), KTable
{
    init
    {
        with(info) {
            stack {
                it.size(100f, 100f).fill()
                image("BadgeRound") {

                }
                if (tgtCharacter != "")
                {
                    image(tgtCharacter) {
                    }
                }
                when (type)
                {
                    InformationType.ACTION ->
                    {
                        image("HelpGrunge") {
                        }
                    }

                    InformationType.RESOURCES ->
                    {
                        image("HeartGrunge") {
                        }
                    }

                    InformationType.CASUALTY ->
                    {
                        image("LightGrunge") {
                        }

                    }

                    else ->
                    {

                    }
                }
            }
        }
    }
}