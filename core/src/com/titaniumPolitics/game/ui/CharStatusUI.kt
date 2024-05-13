package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.stack
import ktx.scene2d.table

//This class is a UI element that displays the player's portrait and their health and will meters.
class CharStatusUI(gameState: GameState) : Table(defaultSkin), KTable
{

    init
    {
        stack {
            it.fillX()
            image(defaultSkin.getDrawable("panel"))
            table {
                add(HealthMeter(gameState)).fill()
                //row()
                //add(WillMeter(gameState)).fill()
            }

        }
        row()
        image(defaultSkin.getDrawable("portrait1")) {
            it.size(200f)
        }
    }

}
