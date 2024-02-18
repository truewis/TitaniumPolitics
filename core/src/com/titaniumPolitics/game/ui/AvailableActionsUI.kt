package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Eat
import com.titaniumPolitics.game.core.gameActions.Wait
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableActionsUI(var gameState: GameState) : Table(defaultSkin), KTable
{
    var titleLabel: Label
    private val docList = HorizontalGroup()
    val options: KStack

    init
    {
        titleLabel = Label("AvailableActions", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()

        options = stack {
            add(ExamineUI(this@AvailableActionsUI.gameState))
            it.fill()
            isVisible = false
        }
        CapsuleStage.instance.onMouseDown.add { x, y ->
            //If x and y are not within the bounds of this UI, hide the option ui.
            val localpos = options.screenToLocalCoordinates(Vector2(x, y))
            if (options.hit(localpos.x, localpos.y, true) == null)
            {
                options.isVisible = false
            }
        }
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
            gameState.player.place.name,
            gameState.playerName
        ).forEach { tobj ->
            //We do not create buttons for these actions, as they are accessible through the main UI.
            if (listOf("Move", "Talk").contains(tobj))
            {
                return@forEach
            }
            val t = scene2d.button {
                textTooltip(tobj, "default") {
                    this.setFontScale(2f)
                    it.manager.initialTime = 1f
                }
                image("question-mark-circle-outline-icon") {
                    it.size(100f)

                    when (tobj)
                    {


                        "Trade" ->
                        {
                            this.setDrawable(defaultSkin, "hand-shake-icon")
                        }

                        "Examine" ->
                        {
                            this.setDrawable(defaultSkin, "magnifying-glass-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    this@AvailableActionsUI.options.isVisible = true
                                }
                            }
                            )
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "Sleep" ->
                        {
                            this.setDrawable(defaultSkin, "closed-eye-icon")
                        }

                        "Repair" ->
                        {
                            this.setDrawable(defaultSkin, "hammer-line-icon")
                        }

                        "UnofficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "boxes-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    ResourceTransferUI.instance.isVisible = true
                                    ResourceTransferUI.instance.refresh(this@AvailableActionsUI.gameState.player.place.resources)
                                    ResourceTransferUI.instance.mode = "unofficial"
                                }
                            })
                        }

                        "OfficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "boxes-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    ResourceTransferUI.instance.isVisible = true
                                    ResourceTransferUI.instance.refresh(this@AvailableActionsUI.gameState.player.place.resources)
                                    ResourceTransferUI.instance.mode = "official"
                                }
                            })
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