package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image

class CharStatusUI(gameState: GameState) : Table(defaultSkin), KTable
{

    init
    {
        add(HealthMeter(gameState)).fill()
        row()
        add(WillMeter(gameState)).fill()
        row()
        image(defaultSkin.getDrawable("capsuleDevDanger")) {
            it.size(200f)
        }
    }

}
