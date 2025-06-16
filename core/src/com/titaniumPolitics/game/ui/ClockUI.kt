package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin.defaultSkin

class ClockUI(gameState: GameState) : Table(defaultSkin) {
    val l: Label = Label((gameState.formatClock()), defaultSkin, "console")

    init {
        l.setFontScale(2f)
        add(l).growX()

        gameState.timeChanged += { _, y ->
            Gdx.app.postRunnable { l.setText(gameState.formatClock()) }
        }
    }

    companion object
}
