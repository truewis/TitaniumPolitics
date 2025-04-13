package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class ResourceTransferUI(gameState: GameState, override var actionCallback: (GameAction) -> Unit) :
    WindowUI("ResourceTransferTitle"), ActionUI
{
    private val dataTable = Table()
    private val targetTable = Table()

    //Determines if the transfer is official or not.
    var mode: String = "official"
    var current = hashMapOf<String, Int>()
    var target = hashMapOf<String, Int>()
    var toWhere = ""
    var modeLabel: Label
    var placeButton: Button

    init
    {
        isVisible = false
        instance = this
        val currentResourcePane = ScrollPane(dataTable)
        currentResourcePane.setScrollingDisabled(false, false)

        val targetResourcePane = ScrollPane(targetTable)
        targetResourcePane.setScrollingDisabled(false, false)
        val st = stack {
            it.grow()
            table {
                this@ResourceTransferUI.modeLabel = label("Transfer Mode", "trnsprtConsole") { setFontScale(3f) }
                row()
                //Select place to transfer resources to.
                this@ResourceTransferUI.placeButton = button {
                    it.colspan(2).growX()
                    val placeLabel = label("Transfer Resource To:", "trnsprtConsole") { setFontScale(3f) }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            PlaceSelectionUI.instance.isVisible = true
                            PlaceSelectionUI.instance.refresh()
                            PlaceSelectionUI.instance.selectedPlaceCallback = {
                                placeLabel.setText("Transfer Resource To: $it")
                                this@ResourceTransferUI.toWhere = it
                            }
                        }
                    })
                }
                row()
                add(currentResourcePane)
                add(targetResourcePane)
                row()
                button {
                    it.fill()
                    label("Transfer") {
                        setAlignment(Align.center)
                        setFontScale(3f)
                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            if (this@ResourceTransferUI.mode == "official")
                            {
                                this@ResourceTransferUI.actionCallback(
                                    OfficialResourceTransfer(
                                        gameState.playerName,
                                        gameState.player.place.name
                                    ).apply {
                                        this.resources = this@ResourceTransferUI.target
                                        this.toWhere = this@ResourceTransferUI.toWhere
                                    }
                                )
                            } else if (this@ResourceTransferUI.mode == "unofficial")
                            {
                                this@ResourceTransferUI.actionCallback(
                                    UnofficialResourceTransfer(
                                        gameState.playerName,
                                        gameState.player.place.name
                                    ).apply {
                                        this.resources = this@ResourceTransferUI.target
                                        this.toWhere = this@ResourceTransferUI.toWhere
                                    }
                                )
                            } else if (this@ResourceTransferUI.mode == "private")
                            {
                                this@ResourceTransferUI.actionCallback(
                                    UnofficialResourceTransfer(
                                        gameState.playerName,
                                        gameState.player.place.name
                                    ).apply {
                                        this.resources = this@ResourceTransferUI.target
                                        this.toWhere = this@ResourceTransferUI.toWhere
                                        this.fromHome = true
                                    }
                                )
                            }
                            this@ResourceTransferUI.isVisible = false
                        }
                    })
                }
                button {
                    it.fill()
                    label("Cancel") {
                        setAlignment(Align.center)
                        setFontScale(3f)

                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@ResourceTransferUI.isVisible = false
                        }
                    })
                }
            }
        }
        content.add(st).grow()


    }


    fun refresh(
        mode: String,
        action: (GameAction) -> Unit,
        current: HashMap<String, Int>,
        target: HashMap<String, Int> = hashMapOf(),
    )
    {
        this.actionCallback = action
        this.current = current
        this.target = target
        this.mode = mode
        this.modeLabel.setText("Transfer: $mode")
        placeButton.isVisible = mode != "private"
        dataTable.clear()
        dataTable.apply {
            add(table {
                current.forEach { (resourceName, resourceAmount) ->
                    if (resourceAmount != 0)
                    {
                        label("$resourceName: $resourceAmount", "trnsprtConsole") {
                            setFontScale(2f)
                            setAlignment(Align.center)
                            addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    current[resourceName] = current[resourceName]!! - 1
                                    target[resourceName] = (target[resourceName] ?: 0) + 1
                                    this@ResourceTransferUI.refresh(mode, action, current, target)
                                }
                            })
                        }
                        row()
                    }
                }
            })
        }

        targetTable.clear()
        targetTable.apply {
            add(table {
                target.forEach { (resourceName, resourceAmount) ->
                    if (resourceAmount != 0)
                    {
                        label("$resourceName: $resourceAmount", "trnsprtConsole") {
                            setFontScale(2f)
                            setAlignment(Align.center)
                            addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    target[resourceName] = target[resourceName]!! - 1
                                    current[resourceName] = (current[resourceName] ?: 0) + 1
                                    this@ResourceTransferUI.refresh(mode, action, current, target)
                                }
                            })
                        }
                        row()
                    }
                }
            })
        }
    }

    companion object
    {
        //Singleton
        lateinit var instance: ResourceTransferUI
    }


}