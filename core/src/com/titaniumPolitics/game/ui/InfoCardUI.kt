package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.*
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin


//This UI is used for both meetings and conferences.
//It appears as cards in the lower half of the screen.
//Once they are deployed, they appear as InfoBubbleUIs.
class InfoCardUI(var gameState: GameState) : Table(defaultSkin), KTable
{

    val infoTitle = Label("Info", defaultSkin, "trnsprtConsole")

    init
    {
        infoTitle.setFontScale(2f)



        add(infoTitle)
    }

    fun refresh(info: Information)
    {
        infoTitle.setText(info.name.substring(0, 10))
    }


}