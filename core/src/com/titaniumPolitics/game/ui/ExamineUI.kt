package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Examine
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class ExamineUI(var gameState: GameState) : Table(defaultSkin) {
    var titleLabel: Label
    private val docList = HorizontalGroup()

    init {
        titleLabel = Label("Options", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        docList.grow()
        docList.addActor(scene2d.container {
            button("document") {
                image("UserGrunge") {
                    it.size(70f)
                    this@button.addListener(object : ClickListener() {
                        override fun clicked(
                            event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                            x: Float,
                            y: Float
                        ) {
                            GameEngine.acquireCallback(
                                Examine(
                                    gameState.playerName,
                                    gameState.player.place.name
                                ).also { it.what = "HR" }
                            )
                            this@ExamineUI.isVisible = false
                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Investigating")
                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
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
                    this@button.addListener(object : ClickListener() {
                        override fun clicked(
                            event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                            x: Float,
                            y: Float
                        ) {
                            GameEngine.acquireCallback(
                                Examine(
                                    gameState.playerName,
                                    gameState.player.place.name
                                ).also { it.what = "apparatus" }
                            )
                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Investigating")
                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
                            this@ExamineUI.isVisible = false
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
                    this@button.addListener(object : ClickListener() {
                        override fun clicked(
                            event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                            x: Float,
                            y: Float
                        ) {
                            GameEngine.acquireCallback(
                                Examine(
                                    gameState.playerName,
                                    gameState.player.place.name
                                ).also { it.what = "resources" }
                            )
                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Investigating")
                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
                            this@ExamineUI.isVisible = false
                        }
                    }
                    )
                }

            }
            size(100f, 100f)
        })
        docList.addActor(scene2d.container {
            button("document") {
                image("X") {
                    it.size(70f)
                    this@button.addListener(object : ClickListener() {
                        override fun clicked(
                            event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                            x: Float,
                            y: Float
                        ) {
                            this@ExamineUI.isVisible = false
                        }
                    }
                    )
                }

            }
            size(100f, 100f)
        })
        add(docList).size(300f, 100f)
    }

    override fun setVisible(visible: Boolean) {
        CharacterPortraitsUI.instance.isVisible = !visible
        super.setVisible(visible)
    }


}