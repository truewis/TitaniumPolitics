package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class PortraitUI(character: String, var gameState: GameState, scale: Float) : Table(defaultSkin), KTable
{
    val portrait = scene2d.image("UserGrunge") {
        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
        {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
            {
                //Open Character Marker UI
                CharacterInteractionWindowUI.instance.isVisible = true
                val coord = localToStageCoordinates(Vector2(x, y))
                CharacterInteractionWindowUI.instance.refresh(coord.x, coord.y, this@PortraitUI.tgtCharacter)
            }
        })
    }
    var tgtCharacter = character
        set(value)
        {
            field = value
            try
            {
                portrait.setDrawable(defaultSkin, value)
            } catch (e: Exception)
            {
                println("Portrait Image Error: $value")
            }
        }
    val speech = scene2d.label("Hello", "trnsprtConsole") {
        setFontScale(3f)
    }
    val bubble = scene2d.stack {
        add(this@PortraitUI.speech)
    }
    val theEmoji = scene2d.image("HelpGrunge")

    val refresh = { state: GameState ->

        //If there is an action that was taken by the character last turn, display a script on the portrait.
        val action =
            state.informations.values.firstOrNull { it.tgtCharacter == tgtCharacter && it.type == InformationType.ACTION && it.creationTime == state.time - 1 }
        if (action != null && ReadOnly.script(action.action!!.javaClass.simpleName) != null)
        {
            bubble.isVisible = true
            speech.setText(ReadOnly.script(action.action!!.javaClass.simpleName))
        } else
        {
            bubble.isVisible = false
        }

        //Display emoji based on event conditions.
        if (state.eventSystem.dataBase.any { it.displayEmoji(tgtCharacter) })
        {
            displayEmojiOnPortrait("HelpGrunge")
        } else
        {
            displayEmojiOnPortrait("")
        }
    }

    init
    {
        bubble.isVisible = false
        //mMeter.isVisible = false
        theEmoji.isVisible = false
        add(bubble).growX()
        row()
        add(theEmoji).growX().size(100f)
        row()
        add(portrait).size(500f * scale, 700f * scale)
        gameState.updateUI += refresh
        refresh(gameState)

    }

    override fun remove(): Boolean
    {
        gameState.updateUI -= refresh
        return super.remove()
    }


    fun displayEmojiOnPortrait(emojiTexture: String)
    {
        if (emojiTexture == "")
        {
            theEmoji.isVisible = false
        } else
        {
            //theEmoji.setDrawable(defaultSkin, emojiTexture)
            theEmoji.isVisible = true
        }
    }
}