package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class WillMeter (gameState: GameState) : Table(defaultSkin) {
    var l: Label

    init {
        l = Label("", defaultSkin, "trnsprtConsole")
        l.setFontScale(2f)
        val b = TextButton("WILL", defaultSkin)
        add(b)
        add(l)

        gameState.updateUI+={y->
            Gdx.app.postRunnable {setValue(y.getMutuality(gameState.playerAgent, gameState.playerAgent).toInt())}}
    }
    fun setValue(value:Int) {
        l.setText(value.toString().padStart(2, '0'))
    }
}
