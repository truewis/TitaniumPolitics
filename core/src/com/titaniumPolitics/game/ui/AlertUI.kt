package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.scene2d
import ktx.scene2d.*

class AlertUI(var gameState: GameState) : Table(defaultSkin)
{
    private val docList = VerticalGroup()
    private val previousInformation = hashSetOf<String>()

    init
    {
        instance = this
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.updateUI += { _ -> refreshList(); }
    }

    fun addAlert(type: String, action: () -> Unit = {})
    {
        if (type in listOf("vital", "hunger", "thirst") && docList.children.none {
                (it as AlertPanelUI).type == type
            })//Only one alert of each type is visible at a time.
            docList.addActor(AlertPanelUI(type, action, docList))
        else if (type !in listOf("vital", "hunger", "thirst"))
            docList.addActor(AlertPanelUI(type, action, docList))
        if (!isVisible)
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
                //Decide whether to show the alert based on the type of information.
                val info = gameState.informations[it]!!
                when (info.type)
                {
                    InformationType.CASUALTY ->
                    {
                        addAlert("accident") {
                            InformationViewUI.instance.refresh(gameState, "creationTime")
                            InformationViewUI.instance.isVisible = true
                        }
                    }

                    InformationType.ACTION ->
                    {
                        if (info.tgtCharacter == gameState.playerName//Ignore my actions, they are not surprising.
                            || setOf(
                                "Move",
                                "Wait"
                            ).contains(info.action!!.javaClass.simpleName)
                        )//Ignore boring actions, even if they are not mine.
                        {
                            addAlert("newInfo") {
                                InformationViewUI.instance.refresh(gameState, "creationTime")
                                InformationViewUI.instance.isVisible = true
                            }
                        }
                    }

                    InformationType.APPARATUS_DURABILITY ->
                    {
                        addAlert("apparatus") {
                            InformationViewUI.instance.refresh(gameState, "creationTime")
                            InformationViewUI.instance.isVisible = true
                        }
                    }

                    else ->
                    {
                        //Do nothing.
                    }
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