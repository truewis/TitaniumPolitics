package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//TODO: Make this scrollable to deal with many characters.
//This UI is used to display the portraits of the characters in the current place.
class CharactersInPlaceUI(var gameState: GameState) : Table(defaultSkin) {
    private val portraits = arrayListOf<PortraitUI>()

    //Also check MeetingUI for similar code.
    private val animationQueue = ArrayDeque<Action>()
    var onAnimationEnd: () -> Unit = {}

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
                Logger.write(
                    "CharacterPortraits: Non-player character action detected: ${action.sbjCharacter} performed ${action::class.simpleName}",
                    Logger.LogLevel.INFO
                )
                //Block the game engine until the animation is done.
                runBlocking {
                    animationQueue.add(
                        Actions.run {
                            portraits.forEach { portrait ->
                                if (portrait.tgtCharacter == action.sbjCharacter) {
                                    portrait.speechUI.displaySpeech(action)
                                }
                            }
                        }
                    )
                    suspendCoroutine { continuation ->
                        Gdx.app.postRunnable {
                            onAnimationEnd =
                                {
                                    try {
                                        continuation.resume(Unit)
                                    } catch (e: IllegalStateException) {
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
//
//    override fun act(delta: Float) {
//        super.act(delta)
//        if (isAnimating) {
//            //OtherCharacterProgressBackgroundUI.instance.isVisible = true
//        } else if (animationQueue.isNotEmpty()) {
//
//            isAnimating = true
//        } else {
//            // No animation to play, hide the background UI.
//            //OtherCharacterProgressBackgroundUI.instance.isVisible = false
//        }
//    }

    fun refresh(place: String) {
        portraits.forEach { it.remove() }
        portraits.clear()
        gameState.places[place]!!.characters.forEach {

            //Player cannot see themselves.
            if (it != gameState.playerName && it in gameState.knownCharactersToPlayer)
                addCharacterPortrait(it)
        }
        placeCharacterPortrait()
    }

    private fun addCharacterPortrait(characterName: String) {
        val portrait = PortraitUI(characterName, gameState)
        portrait.speechUI.onSpeechEnd += {
            if (animationQueue.isEmpty())
                onAnimationEnd()
            else
                this@CharactersInPlaceUI.addAction(animationQueue.removeFirst())
        }
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
        lateinit var instance: CharactersInPlaceUI
    }
}