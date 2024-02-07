package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.progressBar
import ktx.scene2d.scene2d
import ktx.scene2d.stack

class HealthMeter(gameState: GameState) : Table(defaultSkin)
{
    val bar = scene2d.progressBar(0f, 1f, 0.01f, false, "default-horizontal")

    init
    {
        val b = Image(defaultSkin, "heart-beat-icon")
        b.color = Color.WHITE
        add(b).size(50f)
        add(bar).growX()
        gameState.updateUI += { y ->
            Gdx.app.postRunnable { setValue(y.characters[y.playerAgent]!!.health) }
        }
    }

    fun setValue(value: Int)
    {
        bar.value = value.toFloat() / 100
        bar.updateVisualValue()
    }
}
