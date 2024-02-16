package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class ResourceTransferUI(gameState: GameState) : Table(defaultSkin), KTable
{
    private val dataTable = Table()
    private val targetTable = Table()

    //Determines if the transfer is official or not.
    var mode: String = "official"

    init
    {
        isVisible = false
        instance = this
        val currentResourcePane = ScrollPane(dataTable)
        currentResourcePane.setScrollingDisabled(false, false)

        val targetResourcePane = ScrollPane(dataTable)
        currentResourcePane.setScrollingDisabled(false, false)
        stack {
            it.grow()
            image("capsuleDevLabel1") {
            }
            table {
                add(currentResourcePane)
                add(targetResourcePane)
                row()
                button {
                    label("Transfer") {
                        setAlignment(Align.center)
                        addListener(object : ClickListener()
                        {
                            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                            {
                                if (this@ResourceTransferUI.mode == "official")
                                {
                                    GameEngine.acquireCallback(
                                        OfficialResourceTransfer(
                                            gameState.playerAgent,
                                            gameState.characters[gameState.playerAgent]!!.place.name
                                        ).apply {

                                        }
                                    )
                                }
                            }
                        })
                    }
                }
                button {
                    label("Cancel") {
                        setAlignment(Align.center)
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
        }


    }


    fun refresh(current: HashMap<String, Int>, target: HashMap<String, Int> = hashMapOf())
    {
        dataTable.clear()
        dataTable.apply {
            add(table {
                current.forEach { (resourceName, resourceAmount) ->
                    label("$resourceName: $resourceAmount") {
                        setAlignment(Align.center)
                        addListener(object : ClickListener()
                        {
                            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                            {
                                current[resourceName] = current[resourceName]!! - 1
                                target[resourceName] = (target[resourceName] ?: 0) + 1
                                this@ResourceTransferUI.refresh(current, target)
                            }
                        })
                    }
                    row()
                }
            })
        }

        targetTable.clear()
        targetTable.apply {
            add(table {
                target.forEach { (resourceName, resourceAmount) ->
                    label("$resourceName: $resourceAmount") {
                        setAlignment(Align.center)
                        addListener(object : ClickListener()
                        {
                            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                            {
                                target[resourceName] = target[resourceName]!! - 1
                                current[resourceName] = (current[resourceName] ?: 0) + 1
                                this@ResourceTransferUI.refresh(current, target)
                            }
                        })
                    }
                    row()
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