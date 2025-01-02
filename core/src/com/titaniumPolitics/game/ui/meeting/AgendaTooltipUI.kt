package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
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
                    label(agenda.type.toString(), "black") {
                        it.growX()
                        setFontScale(2f)
                    }
                }
            }
            row()
            label(agenda.subjectParams.toString()) {
                it.size(350f, 150f)
                setFontScale(2f)
                setAlignment(Align.topLeft)
                wrap = true
            }
            if (agenda.attachedRequest != null)
            {
                row()
                label(agenda.attachedRequest!!.toString()) {
                    it.size(350f, 150f)
                    setFontScale(2f)
                    setAlignment(Align.topLeft)
                    wrap = true
                }
            }


        }
    }

})
{
    init
    {
        manager.initialTime = 0.5f
    }

}