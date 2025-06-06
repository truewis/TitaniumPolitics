package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.progressBar
import ktx.scene2d.scene2d

class WillMeter(gameState: GameState) : Table(defaultSkin)
{
    val bar = scene2d.progressBar(0f, 1f, 0.01f, false, "default-horizontal")

    init
    {
        val b = Image(defaultSkin, "confused-line-icon")
        b.color = Color.WHITE
        add(b).size(50f)
        add(bar).growX()
        gameState.updateUI += { y ->
            setValue(y.getMutuality(gameState.playerName, gameState.playerName).toInt())
        }
    }

    fun setValue(value: Int)
    {
        bar.value = value.toFloat() / 100
        bar.updateVisualValue()
    }
}
