package com.capsulezero.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.capsulezero.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class WillMeter (gameState: GameState) : Table(defaultSkin) {
    var l: Label

    init {
        l = Label("", defaultSkin, "trnsprtConsole")
        l.setFontScale(2f)
        val b = TextButton("의지", defaultSkin)
        add(b)
        add(l)

        gameState.updateUI+={y->setValue(y.characters[gameState.playerAgent]!!.will)}
    }
    fun setValue(value:Int) {
        l.setText(value.toString().padStart(2, '0'))
    }
}
