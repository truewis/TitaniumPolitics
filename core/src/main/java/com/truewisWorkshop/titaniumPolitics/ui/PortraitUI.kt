package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.widget.SimplePortraitUI
import com.titaniumPolitics.game.ui.widget.SpeechUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class PortraitUI(character: String, var gameState: GameState) : Table(defaultSkin), KTable {
    val portrait = SimplePortraitUI(character, 1f, true)
    val speechUI = SpeechUI()
    var tgtCharacter = character
        set(value) {
            field = value
            portrait.tgtCharacter = tgtCharacter
        }


    val refresh = { state: GameState ->
        speechUI.clearSpeech()
        //Display emoji based on event conditions.
        speechUI.displayEmoji(state.eventSystem.displayEmoji(tgtCharacter))
    }


    init {
        tgtCharacter = character
        add(this@PortraitUI.speechUI).size(600f, 200f).fill().padBottom(-50f)
        row()
        add(portrait).size(500f, 1000f).fill()
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