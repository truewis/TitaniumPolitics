package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class SimpleTextTooltipUI(text: String, width: Float = 350f) : Tooltip<Table>(scene2d.table {
    stack {
        container(scene2d.image("TooltipShadow10p") {
            setColor(0f, 0f, 0f, 0.7f)
            touchable = Touchable.disabled//This is a shadow outside the tooltip
        }) {
            fill()
            pad(-50f)
        }
        it.size(350f).growY()
        image("BlackPx")

        image("NoiseBackground") {
            setColor(1f, 1f, 1f, 0.1f)
        }
        image("PanelDottedShade700x700") {
            setColor(0f, 0f, 0f, 1f)
        }
        table {
            val PADDING = 3f
            pad(PADDING)
            label(text, "description") {
                it.size(width - 2 * PADDING, 0f).growY()
                setFontScale(0.3f)
                setAlignment(Align.topLeft)
                wrap = true
            }
        }

        image("Stroke500x500") {
            setColor(0f, 0f, 0f, 1f)
        }
    }

}) {
    init {
        manager.initialTime = 0.5f

    }

}