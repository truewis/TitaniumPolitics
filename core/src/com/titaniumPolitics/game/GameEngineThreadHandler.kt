package com.titaniumPolitics.game

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GameEngineThreadHandler {
    companion object {
        var engine: GameEngine? = null
        var thread: Thread? = null

        fun startEngine(gameState: GameState) {
            engine = GameEngine(gameState)
            thread = thread(start = true) {
                val engine = GameEngine(gameState)
                engine.onObserverCall += {
                    runBlocking {
                        suspendCoroutine { cont ->
                            Gdx.app.postRunnable {
                                val current =
                                    gameState.updateUI.clone() as ArrayList<(GameState) -> Unit> //Clone the list to prevent concurrent modification, because updateUI can be modified by UI elements during the update.
                                current.forEach { it(gameState) }//Update UI
                                cont.resume(Unit)
                            }
                        }
                    }

                }
                engine.startGame()
            }
        }

        fun stopEngine() {
            engine?.destroy()
            thread?.interrupt()
        }
    }

}