package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

class ResourceTooltipUI(itemName: String) : Tooltip<Table>(scene2d.table {
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
            val PADDING = 3f
            pad(PADDING)
            stack {
                it.size(350f - PADDING * 2, 50f)
                image("TooltipTitle")
                table {
                    label(ReadOnly.itemProp(itemName), "description") {
                        it.growX()
                        setFontScale(0.4f)
                        color = Color.BLACK
                    }
                    // Uncomment to display duration of the resource
                    //add(TimeAmountUI(ReadOnly.constInt(itemName + "Duration"))).fill()
                }
            }
            row()
            label(ReadOnly.itemProp("$itemName-desc"), "description") {
                it.size(350f - PADDING * 2, 200f)
                setFontScale(0.25f)
                setAlignment(Align.topLeft)
                wrap = true
            }
            //Optional: display invalid reason text here
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

        image("Stroke500x500") {
            setColor(0f, 0f, 0f, 1f)
        }
    }

}) {
    init {
        manager.initialTime = 0.5f
    }

}