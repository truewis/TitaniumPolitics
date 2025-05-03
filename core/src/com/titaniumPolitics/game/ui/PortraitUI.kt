package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class PortraitUI(character: String, var gameState: GameState, scale: Float) : Table(defaultSkin), KTable
{
    var displayTextBubble = true
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
            //TODO: Also check SimplePortraitUI for this.
            field = value
            try
            {
                portrait.drawable = TextureRegionDrawable(
                    CapsuleStage.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                        ReadOnly.charJson[tgtCharacter]!!.jsonObject["image"]!!.jsonPrimitive.content,
                        Texture::class.java
                    )!!
                )
            } catch (e: Exception)
            {
                println("Portrait Image Error: $value")
            }
        }
    val speech = scene2d.label("Hello", "console") {
        setFontScale(3f)
        wrap = true
        width = 400f
    }
    val bubble = scene2d.stack {
        image("TooltipTitle") {
        }
        add(this@PortraitUI.speech)
    }
    val theEmoji = scene2d.image("HelpGrunge")

    val refresh = { state: GameState ->

        //If there is an action that was taken by the character last turn, display a script on the portrait.
        val actionInfo =
            state.informations.values.firstOrNull { it.tgtCharacter == tgtCharacter && it.type == InformationType.ACTION && it.creationTime == state.time - 1 }
        if (actionInfo != null && ReadOnly.script(actionInfo.action!!.javaClass.simpleName) != null && displayTextBubble)
        {
            bubble.isVisible = true
            speech.setText(ReadOnly.script(actionInfo.action!!.javaClass.simpleName, actionInfo.action))
        } else
        {
            bubble.isVisible = false
        }

        //Display emoji based on event conditions.
        if (state.eventSystem.displayEmoji(tgtCharacter))
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
        theEmoji.isVisible = emojiTexture != ""
        //theEmoji.setDrawable(defaultSkin, emojiTexture)
    }
}