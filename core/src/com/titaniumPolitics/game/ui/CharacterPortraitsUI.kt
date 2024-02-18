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


//This UI is used to display the portraits of the characters in the current place.
class CharacterPortraitsUI(var gameState: GameState) : Table(defaultSkin)
{
    val currentCharacerMarkerWindow = CharacterInteractionWindowUI(gameState, null)
    val portraits = arrayListOf<Actor>()

    init
    {
        instance = this
        gameState.updateUI.add {
            refresh(it.characters[gameState.playerAgent]!!.place.name)
        }
        addActor(currentCharacerMarkerWindow)
    }

    fun refresh(place: String)
    {
        portraits.forEach { it.remove() }
        portraits.clear()
        gameState.places[place]!!.characters.forEach {

            //Player cannot see themselves.
            if (it != gameState.playerAgent)
                addCharacterPortrait(it)
        }
        placeCharacterPortrait()
    }

    private fun addCharacterPortrait(characterName: String)
    {

        val portrait = scene2d.image("raincoat-icon") {
            if (defaultSkin.has(characterName, Drawable::class.java))
                this.setDrawable(defaultSkin, characterName)
            addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    //Open Character Marker UI
                    currentCharacerMarkerWindow.isVisible = true
                    currentCharacerMarkerWindow.refresh(x, y, characterName)
                }
            })
        }
        portraits.add(portrait)
        portrait.name = characterName
        addActor(portrait)


    }

    private fun placeCharacterPortrait()
    {
        //Place portraits across the screen so they are not on top of each other.
        portraits.forEach {
            it.setPosition(portraits.indexOf(it) * Gdx.graphics.width.toFloat() / portraits.size, 0f)
        }

    }

    fun displayEmojiOnPortrait(characterName: String, emojiTexture: Texture)
    {
        val characterPortrait = children.find { it.name == characterName }
        if (characterPortrait != null)
        {
            val emoji = Image(emojiTexture)
            addActor(emoji)
        }
    }

    companion object
    {
        lateinit var instance: CharacterPortraitsUI
    }
}