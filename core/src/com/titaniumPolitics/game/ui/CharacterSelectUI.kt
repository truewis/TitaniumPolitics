package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.*
import ktx.scene2d.*

//This UI is used to select a character as a parameter for an action.
//TODO: Select character with a simple dropdown for now. We will eventually have to differentiate between selecting characters in the same place and selecting characters in other places.
class CharacterSelectUI(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{


    companion object
    {
        lateinit var instance: CharacterSelectUI

    }

}