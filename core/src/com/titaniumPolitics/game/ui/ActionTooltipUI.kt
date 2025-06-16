package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.TimeAmountUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class ActionTooltipUI(actionName: String) : Tooltip<Table>(scene2d.table {
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
                    label(ReadOnly.prop(actionName), "description") {
                        it.growX()
                        setFontScale(0.4f)
                        color = Color.BLACK
                    }
                    add(TimeAmountUI(ReadOnly.constInt(actionName + "Duration"))).fill()
                }
            }
            row()
            label(ReadOnly.prop("$actionName-description"), "description") {
                it.size(350f, 200f)
                setFontScale(0.25f)
                setAlignment(Align.topLeft)
                wrap = true
            }
            row()
            label("", "description") {
                it.size(350f, 100f)
                name = "reasonText"
                setFontScale(0.25f)
                color = Color.RED
                setAlignment(Align.topLeft)
                wrap = true
            }
        }
    }

}) {
    init {
        manager.initialTime = 0.5f
    }

    fun displayInvalidReason(reason: String) {
        container.findActor<Label>("reasonText").setText(reason)
    }

}