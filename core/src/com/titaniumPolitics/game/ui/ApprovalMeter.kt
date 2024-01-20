package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class ApprovalMeter(gameState: GameState) : Table(defaultSkin)
{
    var l: Label

    init
    {
        l = Label("", defaultSkin, "trnsprtConsole")
        l.setFontScale(2f)
        val b = TextButton("Approval", defaultSkin)
        add(b)
        add(l)

        gameState.updateUI += { y -> setValue(0/* characters[gameState.playerAgent]!!.approval*/) }
    }

    fun setValue(value: Int)
    {
        l.setText(value.toString().padStart(2, '0'))
    }
}
