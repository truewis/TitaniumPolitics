package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Eat
import com.titaniumPolitics.game.core.gameActions.Wait
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableActionsUI(var gameState: GameState) : Table(defaultSkin)
{
    var titleLabel: Label
    private val docList = HorizontalGroup()

    init
    {
        titleLabel = Label("AvailableActions", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.updateUI += { _ -> Gdx.app.postRunnable { refreshList(); } }
    }


    fun refreshList()
    {
        docList.clear()
        GameEngine.availableActions(
            gameState,
            gameState.characters[gameState.playerAgent]!!.place.name,
            gameState.playerAgent
        ).forEach { tobj ->
            //We do not create buttons for these actions, as they are accessible through the main UI.
            if (listOf("Move", "Talk").contains(tobj))
            {
                return@forEach
            }
            val t = scene2d.button {

                image("question-mark-circle-outline-icon") {
                    it.size(100f)
                    when (tobj)
                    {


                        "Trade" ->
                        {
                            this.setDrawable(defaultSkin, "hand-shake-icon")
                        }

                        "Investigate" ->
                        {
                            this.setDrawable(defaultSkin, "magnifying-glass-icon")
                        }

                        "Wait" ->
                        {
                            this.setDrawable(defaultSkin, "sand-clock-half-line-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    GameEngine.acquireCallback(
                                        Wait(
                                            gameState.playerAgent,
                                            gameState.characters[gameState.playerAgent]!!.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "Eat" ->
                        {
                            this.setDrawable(defaultSkin, "food-dinner-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    GameEngine.acquireCallback(
                                        Eat(
                                            gameState.playerAgent,
                                            gameState.characters[gameState.playerAgent]!!.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "UnofficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "boxes-icon")
                        }

                        "OfficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "boxes-icon")
                        }

                        else ->
                        {
                            this.setDrawable(defaultSkin, "question-mark-circle-outline-icon")
                        }
                    }

                }
            }
            docList.addActor(t)
        }
        isVisible = !docList.children.isEmpty

    }


}