package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.gameActions.GameAction
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin

class ActionButton(var action: GameAction) : Button(Scene2DSkin.defaultSkin), KTable {
    init {
        this.addListener(object : ClickListener() {
            override fun clicked(
                event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                x: Float,
                y: Float
            ) {
                GameEngine.acquireCallback(action)
            }
        })
    }

    fun refresh() {
        this.isDisabled = !action.isValid()

    }
}