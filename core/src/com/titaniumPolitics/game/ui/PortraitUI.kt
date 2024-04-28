package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class PortraitUI(var tgtCharacter: String, var gameState: GameState) : Table(defaultSkin), KTable
{
    var mMeter = MutualityMeter(gameState, tgtCharacter = tgtCharacter, who = gameState.playerName)
    val speech = scene2d.label("Hello", "trnsprtConsole")
    val bubble = scene2d.stack {
        image("panel")
        add(this@PortraitUI.speech)
    }
    val portrait = scene2d.image("raincoat-icon") {
        if (defaultSkin.has(this@PortraitUI.tgtCharacter, Drawable::class.java))
            this.setDrawable(defaultSkin, this@PortraitUI.tgtCharacter)
        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Open Character Marker UI
                CharacterInteractionWindowUI.instance.isVisible = true
                CharacterInteractionWindowUI.instance.refresh(x, y, this@PortraitUI.tgtCharacter)
            }
        })
    }

    init
    {
        bubble.isVisible = false
        add(bubble).growX()
        row()
        add(mMeter).growX()
        row()
        add(portrait).size(500f, 700f)
        gameState.updateUI += { state ->
            //If there is an action that was taken by the character last turn, display a script on the portrait.
            val action =
                state.informations.values.firstOrNull { it.tgtCharacter == tgtCharacter && it.type == "action" && it.creationTime == state.time - 1 }
            if (action != null && ReadOnly.script(action.action) != null)
            {
                bubble.isVisible = true
                speech.setText(ReadOnly.script(action.action))
            } else
            {
                bubble.isVisible = false
            }

        }

    }

    fun displayEmojiOnPortrait(characterName: String, emojiTexture: Texture)
    {
        val emoji = Image(emojiTexture)
        addActor(emoji)

    }
}