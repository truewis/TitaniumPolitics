package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.scene2d

class CharacterPortraitsUI(var gameState: GameState) : Table(Scene2DSkin.defaultSkin)
{

    init
    {
        instance = this
    }

    fun refresh(place: String)
    {
        clear()
        gameState.places[place]!!.characters.forEach {
            addCharacterPortrait(it)
        }
    }

    private fun addCharacterPortrait(characterName: String)
    {
        val portrait = scene2d.image(characterName)
        portrait.name = characterName
        addActor(portrait)
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