package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
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
    val speech = scene2d.label("Hello", "trnsprtConsole") {
        setFontScale(3f)
    }
    val bubble = scene2d.stack {
        add(this@PortraitUI.speech)
    }
    val theEmoji = scene2d.image("HelpGrunge")
    val portrait = scene2d.image("UserGrunge") {
        try
        {
            this.setDrawable(defaultSkin, this@PortraitUI.tgtCharacter)
        } catch (e: Exception)
        {
            println("Portrait Image Error: ${this@PortraitUI.tgtCharacter}")
        }
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

    val refresh = { state: GameState ->

        //If there is an action that was taken by the character last turn, display a script on the portrait.
        val action =
            state.informations.values.firstOrNull { it.tgtCharacter == tgtCharacter && it.type == "action" && it.creationTime == state.time - 1 }
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
        add(mMeter)
        row()
        add(portrait).size(500f, 700f)
        gameState.updateUI += refresh
        refresh(gameState)

    }

    override fun remove(): Boolean
    {
        mMeter.remove()
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