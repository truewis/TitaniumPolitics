package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class PortraitUI(character: String, var gameState: GameState, scale: Float) : Table(defaultSkin), KTable {
    var displayTextBubble = true
    val portrait = scene2d.image("UserGrunge") {
        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                //Open Character Marker UI
                CharacterInteractionWindowUI.instance.isVisible = true
                val coord = localToStageCoordinates(Vector2(x, y))
                CharacterInteractionWindowUI.instance.refresh(coord.x, coord.y, this@PortraitUI.tgtCharacter)
            }
        })
    }
    var tgtCharacter = character
        set(value) {
            //TODO: Also check SimplePortraitUI for this.
            field = value
            try {
                portrait.drawable = TextureRegionDrawable(
                    CapsuleStage.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                        ReadOnly.charJson[tgtCharacter]!!.jsonObject["image"]!!.jsonPrimitive.content,
                        Texture::class.java
                    )!!
                )
            } catch (e: Exception) {
                println("Portrait Image Error: $value")
                portrait.drawable = TextureRegionDrawable(
                    CapsuleStage.instance.assetManager.get(
                        "portraits/default.png",
                        Texture::class.java
                    )!!
                )
            }
        }
    val speech = TypingLabel("", defaultSkin, "description").apply {
        setFontScale(0.5f)
        color = Color.WHITE
        wrap = true
    }
    val bubble = scene2d.stack {
        image("textbubble") {
            setColor(0f, 0f, 0f, 0.7f) // Semi-transparent bubble
        }
        container(this@PortraitUI.speech) {
            fill()
            pad(30f)
        }
    }
    val theEmoji = scene2d.image("HelpGrunge")

    val refresh = { state: GameState ->
        clearSpeech()
        //Display emoji based on event conditions.
        if (state.eventSystem.displayEmoji(tgtCharacter)) {
            displayEmojiOnPortrait("HelpGrunge")
        } else {
            displayEmojiOnPortrait("")
        }
    }

    fun displaySpeech(action: GameAction) {

        if (displayTextBubble) {
            bubble.isVisible = true
            speech.restart(ReadOnly.script(action.javaClass.simpleName, action))
        } else {
            bubble.isVisible = false
        }
    }

    fun clearSpeech() {
        bubble.isVisible = false
        speech.setText("")
    }

    init {
        tgtCharacter = character
        bubble.isVisible = false
        //mMeter.isVisible = false
        theEmoji.isVisible = false
        add(bubble).size(600f, 200f).fill()
        row()
        add(theEmoji).growX().size(100f)
        row()
        add(portrait).size(500f * scale, 700f * scale).fill()
        gameState.updateUI += refresh
        refresh(gameState)

    }

    //Override this method instead of remove, remove is not called properly.
    override fun setParent(parent: Group?) {
        if (parent == null) {
            gameState.updateUI -= refresh
        }
        super.setParent(parent)
    }


    fun displayEmojiOnPortrait(emojiTexture: String) {
        theEmoji.isVisible = emojiTexture != ""
        //theEmoji.setDrawable(defaultSkin, emojiTexture)
    }
}