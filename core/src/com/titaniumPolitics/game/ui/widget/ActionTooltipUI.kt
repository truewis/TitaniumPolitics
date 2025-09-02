package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class ActionTooltipUI(actionName: String, dangerous: Boolean = false) : Tooltip<Table>(scene2d.table {
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
            stack {
                it.size(350f - PADDING * 2, 50f) /*With the padding, they add up to 350.*/
                image("TooltipTitle") {
                    if (dangerous)
                        setColor(0.7f, 0.0f, 0.0f, 1f)
                    else
                        setColor(0.5f, 0.5f, 0.5f, 1f)
                }
                table {
                    val txt = ReadOnly.prop(actionName)
                    label(txt, "description") {
                        it.growX()
                        //If the action name is too long, set the font scale smaller.
                        if (txt.length > 20) {
                            setFontScale(0.25f)
                        } else {
                            setFontScale(0.4f)
                        }
                        color = Color.BLACK
                    }
                    add(
                        TimeAmountUI(
                            ReadOnly.constInt(actionName + "Duration"),
                            unknown = actionName in setOf("Wait", "Sleep")
                        )
                    ).fill()
                }
            }
            row()
            label(ReadOnly.prop("$actionName-description"), "description") {
                it.size(350f - PADDING * 2, 200f - PADDING * 2)
                setFontScale(0.25f)
                setAlignment(Align.topLeft)
                if (dangerous)
                    color = Color.RED
                wrap = true
            }
            row()
            label("", "description") {
                it.size(350f - PADDING * 2, 100f)
                name = "reasonText"
                setFontScale(0.25f)
                color = Color.RED
                setAlignment(Align.topLeft)
                wrap = true
            }
            row()
            label("This is a placeholder.", "description") {
                it.size(350f - PADDING * 2, 100f)
                name = "reasonText"
                setFontScale(0.25f)
                color = Color.YELLOW
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

    fun displayInvalidReason(reason: String) {
        container.findActor<Label>("reasonText").setText(reason)
    }

}