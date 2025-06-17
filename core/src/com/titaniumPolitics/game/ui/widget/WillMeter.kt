package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class WillMeter(gameState: GameState) : Table(defaultSkin) {
    val bar = MeterUI()

    init {
        val b = Image(defaultSkin, "EmoticonSeriousGrunge")
        b.color = Color.WHITE
        add(b).size(40f).pad(10f)
        bar.color = Color.BLUE
        add(bar).size(160f, 40f).fill()
        gameState.updateUI += { y ->
            setValue(y.player.will.toInt())
        }
    }

    fun setValue(value: Int) {
        bar.setValue(value.toFloat() / 100)
    }
}
