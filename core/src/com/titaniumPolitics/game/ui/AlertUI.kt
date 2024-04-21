package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.scene2d
import ktx.scene2d.*

class AlertUI(var gameState: GameState) : Table(defaultSkin)
{
    var titleLabel: Label
    private val docList = VerticalGroup()
    private val previousInformation = hashSetOf<String>()

    init
    {
        instance = this
        titleLabel = Label("Alerts", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.updateUI += { _ -> refreshList(); }
    }

    fun addAlert(type: String, action: () -> Unit = {})
    {
        docList.addActor(scene2d.stack {
            image("panel")
            table {
                when (type)
                {
                    "newInfo" -> image("edit-document-icon") {
                        it.size(36f)
                    }

                    "vital" -> image("heart-beat-icon") {
                        it.size(36f)
                    }

                    "accident" -> image("skull-icon") {
                        it.size(36f)
                    }

                    "hunger" -> image("heart-beat-icon") {
                        it.size(36f)
                    }

                    "thrist" -> image("heart-beat-icon") {
                        it.size(36f)
                    }

                    "meeting" -> image("speaking-bubbles-line-icon") {
                        it.size(36f)
                    }
                }
                label(ReadOnly.prop(type), "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                    this@label.addListener(object : ClickListener()
                    {
                        override fun clicked(event: InputEvent?, x: Float, y: Float)
                        {
                            super.clicked(event, x, y)
                            action()
                        }
                    })
                }
                button {
                    image("close-square-line-icon") {
                        it.size(36f)
                    }
                    this@button.addListener(object : ClickListener()
                    {
                        override fun clicked(event: InputEvent?, x: Float, y: Float)
                        {
                            super.clicked(event, x, y)
                            docList.removeActor(this@stack)
                        }

                    }
                    )

                }
            }
        })
        isVisible = true
    }


    fun refreshList()
    {
        //New Information
        if (previousInformation.isEmpty())
            previousInformation.addAll(gameState.informations.keys.filter {
                gameState.informations[it]!!.knownTo.contains(
                    gameState.playerName
                )
            }.toHashSet()) //TODO: this is a temporary solution. It has to work when the game loads.
        else
        {
            val newInformation = gameState.informations.keys.filter {
                gameState.informations[it]!!.knownTo.contains(
                    gameState.playerName
                )
            }.toHashSet()
            newInformation.removeAll(previousInformation)
            newInformation.forEach {
                if (gameState.informations[it]!!.type == "accident")
                    addAlert("accident") {
                        InformationViewUI.instance.refresh(gameState, "creationTime")
                        InformationViewUI.instance.isVisible = true
                    }
                else if (!(gameState.informations[it]!!.type == "action" && gameState.informations[it]!!.tgtCharacter == gameState.playerName))//Ignore my actions, they are not surprising.
                    addAlert("newInfo") {
                        InformationViewUI.instance.refresh(gameState, "creationTime")
                        InformationViewUI.instance.isVisible = true
                    }
            }
            previousInformation.clear()
            previousInformation.addAll(newInformation)
        }

        //Hunger and Thirst, Vitality
        if (gameState.player.hunger > ReadOnly.const("hungerThreshold"))
            addAlert("hunger")
        if (gameState.player.thirst > ReadOnly.const("thirstThreshold"))
            addAlert("thirst")
        if (gameState.player.health < 20)
            addAlert("vital")

        isVisible = !docList.children.isEmpty

    }

    companion object
    {
        lateinit var instance: AlertUI
    }


}