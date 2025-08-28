package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.widget.SimpleHeadPortraitUI
import com.titaniumPolitics.game.ui.widget.SpeechUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class HeadPortraitUI(character: String, var gameState: GameState) : Table(defaultSkin), KTable {
    val portrait = SimpleHeadPortraitUI(character, true)
    val characterTitle get() = gameState.characters[tgtCharacter]?.generatePositionText() ?: ""
    val speechUI = SpeechUI()
    val speechContainer = scene2d.container(speechUI) {
        size(450f, 150f)
    }
    private val positionLabel = scene2d.label(this@HeadPortraitUI.characterTitle, "docTitle") {
        setFontScale(0.2f)
        color = Color.WHITE
        setAlignment(Align.center)
    }

    //This serves as a refresh trigger for the UI.
    var tgtCharacter = character
        set(value) {
            field = value
            portrait.tgtCharacter = tgtCharacter
            positionLabel.setText(characterTitle)
            if (positionLabel.text.length > 15)
                positionLabel.wrap = true
            else positionLabel.wrap = false
            if (positionLabel.text.length > 30)
                positionLabel.setFontScale(0.15f)
            else positionLabel.setFontScale(0.2f)
        }


    val refresh = { state: GameState ->
        speechUI.clearSpeech()
        //Display emoji based on event conditions.
        speechUI.displayEmojiOnPortrait(state.eventSystem.displayEmoji(tgtCharacter))
    }


    init {
        tgtCharacter = character
        stack {
            it.size(150f, 150f).fill()

            add(this@HeadPortraitUI.portrait)
            container {
                align(Align.bottomLeft)
                size(100f, 30f)
                fill()
                stack {
                    image("white-pixel") {
                        color = Color.BLACK
                    }
                    add(this@HeadPortraitUI.positionLabel)
                }
            }
        }
        addActor(speechContainer)
        speechContainer.setPosition(75f, 225f, Align.center)

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
}