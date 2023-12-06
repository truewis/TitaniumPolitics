package com.capsulezero.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.capsulezero.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class HealthMeter (gameState: GameState) : Table(defaultSkin) {
    var l: Label

    init {
        l = Label("", defaultSkin, "trnsprtConsole")
        l.setFontScale(2f)
        val b = TextButton("체력", defaultSkin)
        add(b)
        add(l)

        gameState.updateUI+={y->
            Gdx.app.postRunnable {setValue(y.characters[gameState.playerAgent]!!.health)}}
    }
    fun setValue(value:Int) {
        l.setText(value.toString().padStart(2, '0'))
    }
}
