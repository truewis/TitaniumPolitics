package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.image
import ktx.scene2d.scene2d

//TODO: Make this scrollable to deal with many characters.
//This UI is used to display the portraits of the characters in the current place.
class CharacterPortraitsUI(var gameState: GameState) : Table(defaultSkin)
{
    val portraits = arrayListOf<PortraitUI>()

    init
    {
        instance = this
        gameState.updateUI.add {
            refresh(it.player.place.name)
        }
    }

    fun refresh(place: String)
    {
        portraits.forEach { it.remove() }
        portraits.clear()
        gameState.places[place]!!.characters.forEach {

            //Player cannot see themselves.
            if (it != gameState.playerName && !it.contains("Anon"))
                addCharacterPortrait(it)
        }
        placeCharacterPortrait()
    }

    private fun addCharacterPortrait(characterName: String)
    {
        val portrait = PortraitUI(characterName, gameState, 1f)
        portraits.add(portrait)
        addActor(portrait)


    }

    //Cf. the same function in MeetingUI
    private fun placeCharacterPortrait()
    {
        //Place portraits across the screen so they are not on top of each other.
        portraits.forEach {
            it.setPosition(
                (portraits.indexOf(it) + 0.5f) * CapsuleStage.instance.width / portraits.size + it.width / 2,
                300f
            )
        }

    }

    companion object
    {
        lateinit var instance: CharacterPortraitsUI
    }
}