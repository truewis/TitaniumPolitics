package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import ktx.scene2d.Scene2DSkin.defaultSkin
import java.lang.Thread.sleep

//TODO: Make this scrollable to deal with many characters.
//This UI is used to display the portraits of the characters in the current place.
class CharacterPortraitsUI(var gameState: GameState) : Table(defaultSkin) {
    val portraits = arrayListOf<PortraitUI>()

    init {
        instance = this
        gameState.updateUI.add {
            refresh(it.player.place.name)
        }
        GameEngine.onBeforeNonPlayerCharacterAction += { action ->

            //Do not play the animation if progress background is visible.
            if (isVisible && !ProgressBackgroundUI.instance.isVisible && portraits.map { it.tgtCharacter }.contains(
                    action.sbjCharacter
                ) && action !is Wait
            ) {
                //If the action is related to the current meeting, play the animation of mutuality arrows.
                Gdx.app.postRunnable {
                    portraits.forEach { portrait ->
                        if (portrait.tgtCharacter == action.sbjCharacter) {
                            portrait.displaySpeech(action)
                        }
                    }
                    Logger.write(
                        "CharacterPortraits: Non-player character action detected: ${action.sbjCharacter} performed ${action::class.simpleName}",
                        Logger.LogLevel.INFO
                    )
                    sleep(1000)//TODO:
                }
            }
        }
    }

    fun refresh(place: String) {
        portraits.forEach { it.remove() }
        portraits.clear()
        gameState.places[place]!!.characters.forEach {

            //Player cannot see themselves.
            if (it != gameState.playerName && !it.contains("Anon"))
                addCharacterPortrait(it)
        }
        placeCharacterPortrait()
    }

    private fun addCharacterPortrait(characterName: String) {
        val portrait = PortraitUI(characterName, gameState)
        portraits.add(portrait)
        addActor(portrait)


    }

    //Cf. the same function in MeetingUI
    private fun placeCharacterPortrait() {
        //Place portraits across the screen so they are not on top of each other.
        portraits.forEach {
            it.setPosition(
                (portraits.indexOf(it) + 0.5f) * CapsuleStage.instance.width / portraits.size + it.width / 2,
                500f
            )
        }

    }

    companion object {
        lateinit var instance: CharacterPortraitsUI
    }
}