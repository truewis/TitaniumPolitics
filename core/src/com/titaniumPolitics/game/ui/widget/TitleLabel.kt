package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.gameActions.GameAction
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack

class TitleLabel(text: String, fontSize: Float = 1f, textColor: Color = WHITE) : Table(Scene2DSkin.defaultSkin),
    KTable {
    val label = scene2d.label(text, "docTitle") {
        setAlignment(com.badlogic.gdx.utils.Align.center)
        color = textColor
        setFontScale(fontSize)
    }

    init {
        stack {
            it.grow()
            add(this@TitleLabel.label)
//            image("TitleBarTiled") {
//                setColor(0f, 0f, 0f, 76 / 256f) // Semi-transparent black
//            }
        }
        row()
        image("Stroke5pxHorizontal") {
            it.growX()
            it.height(5f)
            color = com.badlogic.gdx.graphics.Color.BLACK
        }
    }
}