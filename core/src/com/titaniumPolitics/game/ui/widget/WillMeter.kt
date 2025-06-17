package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack

class WillMeter(gameState: GameState) : Table(defaultSkin), KTable {
    val bar = MeterUI()

    init {
        stack {
            it.grow()
            label("Will", "docTitle") {
                setFontScale(0.2f)
                setAlignment(Align.topLeft)
                color = Color.GRAY
            }
            container(this@WillMeter.bar) {
                padTop(5f)
                size(200f, 40f)
                fill()

            }
        }
        bar.color = Color.BLUE
        gameState.updateUI += { y ->
            setValue(y.player.will.toInt())
        }
    }

    fun setValue(value: Int) {
        bar.setValue(value.toFloat() / 100)
    }
}
