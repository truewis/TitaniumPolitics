package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label

class DivisionUI(div: Party, size: Float = 200f) : Table(Scene2DSkin.defaultSkin), KTable {
    init {
        align(Align.topLeft)
        container {
            image(div.name + "Division") {
                color = LIGHT_GRAY
            }
            size(size)
            align(Align.topLeft)
        }
        row()
        label(ReadOnly.prop(div.name), "docTitle") {
            it.padTop(-15f * size / 200f) /*Division name closer to the logo for aesthetics*/
            setAlignment(Align.top)
            setFontScale(0.2f * size / 200f)
            color = LIGHT_GRAY
        }
    }
}