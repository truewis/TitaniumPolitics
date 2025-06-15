package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.progressBar
import ktx.scene2d.scene2d

class HealthMeter(gameState: GameState) : Table(defaultSkin) {
    val bar = MeterUI()

    init {
        val b = Image(defaultSkin, "AidGrunge")
        b.color = Color.WHITE
        add(b).size(40f)
        add(bar).size(200f, 50f).fill()
        gameState.updateUI += { y ->
            setValue(y.player.health.toInt())
        }
    }

    fun setValue(value: Int) {
        bar.setValue(value.toFloat() / 100)
    }
}
