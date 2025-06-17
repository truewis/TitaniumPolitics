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

class HealthMeter(gameState: GameState) : Table(defaultSkin), KTable {
    val bar = MeterUI()

    init {
        stack {
            it.grow()
            label("Health", "docTitle") {
                setFontScale(0.2f)
                setAlignment(Align.topLeft)
                color = Color.BLACK
            }
            container(this@HealthMeter.bar) {
                padTop(5f)
                size(200f, 40f)
                fill()

            }
        }
        bar.color = Color.GREEN
        gameState.updateUI += { y ->
            setValue(y.player.health.toInt())
        }
    }

    fun setValue(value: Int) {
        bar.setValue(value.toFloat() / 100)
    }
}
