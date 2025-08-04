package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class ExamineUI(var gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI(ReadOnly.prop("examineUI"), gameState, actionCallback), KTable {
    private val docList = HorizontalGroup()

    init {
        docList.grow()
        docList.addActor(scene2d.container {
            button("document") {
                isDisabled = true // Disable this button, as it is not implemented yet.
                image("UserGrunge") {
                    it.size(70f)
                    this@button.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            actionCallback(
                                Examine(
                                    this@ExamineUI.gameState.playerName,
                                    this@ExamineUI.gameState.player.place.name
                                ).also { it.what = "HR" }
                            )
                            ProgressBackgroundUI.instance.setVisibleWithFade(true, "Examine")
                            this@ExamineUI.onClose.forEach { it() }
                        }
                    }
                    )
                }
            }
            size(100f, 100f)
        })
        docList.addActor(scene2d.container {
            button("document") {
                image("CogGrunge") {
                    it.size(70f)
                    this@button.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            actionCallback(
                                Examine(
                                    this@ExamineUI.gameState.playerName,
                                    this@ExamineUI.gameState.player.place.name
                                ).also { it.what = "apparatus" }
                            )
                            ProgressBackgroundUI.instance.setVisibleWithFade(true, "Examine")
                            this@ExamineUI.onClose.forEach { it() }
                        }
                    }
                    )
                }
            }
            size(100f, 100f)

        })
        docList.addActor(scene2d.container {
            button("document") {
                image("TilesGrunge") {
                    it.size(70f)
                    this@button.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            actionCallback(
                                Examine(
                                    this@ExamineUI.gameState.playerName,
                                    this@ExamineUI.gameState.player.place.name
                                ).also { it.what = "resources" }
                            )
                            ProgressBackgroundUI.instance.setVisibleWithFade(true, "Examine")
                            this@ExamineUI.onClose.forEach { it() }
                        }
                    }
                    )
                }

            }
            size(100f, 100f)
        })
        content.add(docList).size(300f, 100f)
    }

    override fun setVisible(visible: Boolean) {
        //CharacterPortraitsUI.instance.isVisible = !visible //Not a good idea, during the meeting CharacterPortraitsUI should be invisible.
        super.setVisible(visible)
    }


}