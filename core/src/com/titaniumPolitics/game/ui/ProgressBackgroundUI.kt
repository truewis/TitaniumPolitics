package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*

class ProgressBackgroundUI(var gameState: GameState, skin: Skin) : Table(skin), KTable {
    var text = "Loading"

    init {
        instance = this
        isVisible = false

        GameEngine.acquireEvent += {
            // If the action is moving, PlaceMarkerWindowUI set this text to "Moving", and shows this UI.
            // We want to hide this UI when next turn starts, so we check the text.
            if (text == ReadOnly.prop("Moving")) {
                setVisibleWithFade(false)
            }
        }

        stack {
            it.grow()
            image("white-pixel") {
                color = Color.BLACK
            }
            table {
                add(ClockUI(this@ProgressBackgroundUI.gameState).apply {
                    cells.first().center()
                    l.setAlignment(Align.center)
                }).size(300f, 100f)
                row()

                label("Loading...", "trnsprtConsole") {
                    setFontScale(3f)
                    setColor(Color.WHITE)
                    setAlignment(Align.center)
                    addAction(
                        Actions.forever(
                            Actions.sequence(
                                Actions.run {
                                    setText(this@ProgressBackgroundUI.text + ".")
                                }, Actions.delay(0.2f),
                                Actions.run {
                                    setText(this@ProgressBackgroundUI.text + "..")
                                }, Actions.delay(0.2f),
                                Actions.run {
                                    setText(this@ProgressBackgroundUI.text + "...")
                                }, Actions.delay(0.2f)

                            )
                        )
                    )
                }
            }
        }

    }

    //set visibility with fade in and out
    fun setVisibleWithFade(visible: Boolean) {
        if (visible) {
            isVisible = true
            addAction(Actions.fadeIn(0.2f))
        } else {
            addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.run { isVisible = false }))
        }
    }


    companion object {
        lateinit var instance: ProgressBackgroundUI
    }

}