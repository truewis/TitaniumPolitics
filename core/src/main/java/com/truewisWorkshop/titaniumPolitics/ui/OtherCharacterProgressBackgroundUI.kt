package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import ktx.scene2d.*

@Deprecated("Not maintained.")
class OtherCharacterProgressBackgroundUI(var gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable {
    private var status = ""
    private val progressLabel: Label

    init {
        instance = this
        isVisible = false

        GameEngine.acquireEvent += {
            // If the action is moving, PlaceMarkerWindowUI set this text to "Moving", and shows this UI.
            // We want to hide this UI when next turn starts, so we check the text.
            //These actions take multiple turns, so we don't hide the UI next turn.
            if (status != "Wait" && status != "Sleep") {//TODO: This is a temporary fix, should be replaced with a better solution.
                setVisibleWithFade(false, status)
            }
        }

        stack {
            it.grow()
            setColor(0f, 0f, 0f, 0.1f)
            image("white-pixel") {
                color = Color.BLACK
            }
            table {
                this@OtherCharacterProgressBackgroundUI.progressLabel = label(ReadOnly.prop("OtherCharacterProgressBackgroundUI-Loading"), "description") {
                    setFontScale(3f)
                    setColor(Color.WHITE)
                    setAlignment(Align.center)

                }
            }
        }

    }

    //set visibility with fade in and out
    fun setVisibleWithFade(visible: Boolean, actionName: String) {
        if (visible) {
            isVisible = true
            addAction(Actions.fadeIn(0f))// No fade in, just show it immediately, but still need to change alpha to 1f here.
            status = actionName
            val displayText =
                if (this@OtherCharacterProgressBackgroundUI.status != "") ReadOnly.prop("ProgressBackgroundUI-" + this@OtherCharacterProgressBackgroundUI.status) else ""
            with(progressLabel) {
                clearActions()
                addAction(
                    Actions.forever(
                        Actions.sequence(
                            Actions.run {
                                setText("$displayText.")
                            }, Actions.delay(0.2f),
                            Actions.run {
                                setText("$displayText..")
                            }, Actions.delay(0.2f),
                            Actions.run {
                                setText("$displayText...")
                            }, Actions.delay(0.2f)

                        )
                    )
                )
            }
        } else {
            if (status != actionName) {
                Logger.write(
                    "Tried to hide OtherCharacterProgressBackgroundUI with action $actionName, but current status is $status"
                )
                return
            }
            addAction(Actions.sequence(Actions.fadeOut(0.5f), Actions.run { isVisible = false }))
        }
    }


    companion object {
        lateinit var instance: OtherCharacterProgressBackgroundUI
    }

}