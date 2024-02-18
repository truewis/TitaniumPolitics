package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Examine
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class ExamineUI(var gameState: GameState) : Table(defaultSkin)
{
    var titleLabel: Label
    private val docList = HorizontalGroup()

    init
    {
        titleLabel = Label("Options", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val docScr = ScrollPane(docList)
        docList.grow()
        docList.addActor(scene2d.button {
            image("teamwork-together-icon") {
                it.size(70f)
                this@button.addListener(object : ClickListener()
                {
                    override fun clicked(
                        event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                        x: Float,
                        y: Float
                    )
                    {
                        GameEngine.acquireCallback(
                            Examine(
                                gameState.playerName,
                                gameState.player.place.name
                            ).also { it.what = "HR" }
                        )
                    }
                }
                )
            }
        })
        docList.addActor(scene2d.button {
            image("settings-line-icon") {
                it.size(70f)
                this@button.addListener(object : ClickListener()
                {
                    override fun clicked(
                        event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                        x: Float,
                        y: Float
                    )
                    {
                        GameEngine.acquireCallback(
                            Examine(
                                gameState.playerName,
                                gameState.player.place.name
                            ).also { it.what = "apparatus" }
                        )
                    }
                }
                )
            }
        })
        docList.addActor(scene2d.button {
            image("cube-line-icon") {
                it.size(70f)
                this@button.addListener(object : ClickListener()
                {
                    override fun clicked(
                        event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                        x: Float,
                        y: Float
                    )
                    {
                        GameEngine.acquireCallback(
                            Examine(
                                gameState.playerName,
                                gameState.player.place.name
                            ).also { it.what = "resources" }
                        )
                    }
                }
                )
            }

        })
        add(docScr).grow()
    }


}