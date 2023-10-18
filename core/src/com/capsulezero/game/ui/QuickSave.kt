package com.capsulezero.game.ui

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.capsulezero.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin

class QuickSave(gameState: GameState) : Table(defaultSkin) {

    init {

        val b = TextButton("Save", defaultSkin)
        b.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                super.clicked(event, x, y)
                gameState.dump()
            }
        })

        add(b)
    }


}
