package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.progressBar
import ktx.scene2d.scene2d
import ktx.scene2d.textTooltip

class MutualityMeter(var gameState: GameState, var tgtCharacter: String, var who: String) : Table(defaultSkin)
{
    val bar = scene2d.progressBar(0f, 1f, 0.01f, false, "default-horizontal")

    val refresh = { state: GameState -> setValue(state.getMutuality(tgtCharacter, who)) }

    init
    {
        val b = Image(defaultSkin, "hand-shake-icon")
        b.color = Color.WHITE
        add(b).size(50f)
        add(bar).growX()
        textTooltip("${(bar.value * 100).toInt()}", "default") {
            this.setFontScale(2f)
            it.manager.initialTime = 0.5f
        }
        gameState.updateUI += refresh
        refresh(gameState)
    }


    override fun remove(): Boolean
    {
        gameState.updateUI -= refresh
        return super.remove()
    }

    fun setValue(value: Double)
    {
        bar.value = (value / 100).toFloat()
        bar.updateVisualValue()
    }
}
