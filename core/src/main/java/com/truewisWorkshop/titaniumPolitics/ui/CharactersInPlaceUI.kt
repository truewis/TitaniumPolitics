package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.coroutines.runBlocking
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//TODO: Make this scrollable to deal with many characters.
//This UI is used to display the portraits of the characters in the current place.
class CharactersInPlaceUI(var gameState: GameState) : Table(defaultSkin) {
    private val portraits = arrayListOf<PortraitUI>()
    private val portraitContainer = Table().apply {
        add().size(1920f * 2, 300f).padLeft(-1920f / 2).fill()
            .center()  //Make the container wide enough to hold many portraits.
    }
    private val scrollPane = ScrollPane(portraitContainer).apply {
        setScrollingDisabled(false, true)
    }

    //Also check MeetingUI for similar code.
    private val animationQueue = ArrayDeque<Action>()
    var onAnimationEnd: () -> Unit = {}

    /**
     * Block the game engine until the animation is done.
     */
    fun flushAnimation() {
        runBlocking {
            suspendCoroutine { continuation ->
                Gdx.app.postRunnable {
                    if (animationQueue.isNotEmpty()) {
                        addAction(animationQueue.removeFirst())
                    }
                    onAnimationEnd =
                        {
                            try {
                                continuation.resume(Unit)
                            } catch (e: IllegalStateException) {
                                // This can happen if the coroutine was already resumed.
                                println("Continuation was already resumed!")
                            }
                        }
                }
            }
        }

    }

    fun addAnimation(action: Action) {
        animationQueue.add(
            Actions.sequence(
                action,
                Actions.run {
                    if (animationQueue.isNotEmpty())
                        addAction(animationQueue.removeFirst())
                    else
                        onAnimationEnd()
                }
            )
        )
    }

    fun setPortraitsColor(color: com.badlogic.gdx.graphics.Color) {
        portraits.forEach {
            it.portrait.portrait.color = color //I know, the object depth is a bit much here, but it is what it is.

        }
    }

    init {
        instance = this
        add(scrollPane).grow()
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
                    "CharactersInPlaceUI: Non-player character action detected: ${action.sbjCharacter} performed ${action::class.simpleName}",
                    Logger.LogLevel.INFO
                )
                portraits.forEach { portrait ->
                    if (portrait.tgtCharacter == action.sbjCharacter) {
                        portrait.speechUI.displaySpeech(action.generateSpeech())
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
        portraits.forEach {
            gameState.characters[it.tgtCharacter]!!.history.lastOrNull()?.let { lastAction ->
                it.speechUI.displayActionEmoji(lastAction.split(';').first())
            }
        }
        //This causes screen shake when characters are moving, so we will not scroll to the first portrait for now. The player can scroll manually if they want to see the portraits.
//        //Scroll to the first portrait.
//        if (portraits.isNotEmpty()) {
//            scrollPane.scrollTo(
//                portraits.first().x, portraits.first().y, portraits.first().width, portraits.first().height
//            )
//        }
    }

    private fun addCharacterPortrait(characterName: String) {
        val portrait = PortraitUI(characterName, gameState)
        portraits.add(portrait)
        portraitContainer.addActor(portrait)


    }

    //Cf. the same function in MeetingUI
    private fun placeCharacterPortrait() {
        //Place portraits across the screen so they are not on top of each other.
        portraits.forEach {
            it.setPosition(
                (portraits.indexOf(it) + 0.5f) * portraitContainer.width / portraits.size + it.width / 2,
                500f
            )
        }

    }

    companion object {
        lateinit var instance: CharactersInPlaceUI
    }
}
