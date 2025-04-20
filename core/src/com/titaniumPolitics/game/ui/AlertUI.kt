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
import com.titaniumPolitics.game.core.gameActions.Move
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.scene2d
import ktx.scene2d.*

class AlertUI(var gameState: GameState) : Table(defaultSkin)
{
    private val docList = VerticalGroup()
    private val previousInformation = hashSetOf<String>()
    private var newInformation = hashSetOf<String>()

    init
    {
        instance = this
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.timeChanged += { old, new -> if (old != new) refreshList() }
        gameState.updateUI += { _ -> displayAlerts(); }
    }

    fun addAlert(type: String, vararg params: String, action: () -> Unit = {})
    {
        if (type in listOf("vital", "hunger", "thirst", "will") && docList.children.none {
                (it as AlertPanelUI).type == type
            })//Only one alert of each type is visible at a time.
            docList.addActor(AlertPanelUI(type, action, docList, *params))
        else if (type !in listOf("vital", "hunger", "thirst", "will"))
            docList.addActor(AlertPanelUI(type, action, docList, *params))
        if (!isVisible)
            isVisible = true
    }

    //Sort which information is new and which is old.
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
            newInformation = gameState.informations.keys.filter {
                gameState.informations[it]!!.knownTo.contains(
                    gameState.playerName
                )
            }.toHashSet()
            newInformation.removeAll(previousInformation)
            previousInformation.addAll(newInformation)
        }

    }

    fun displayAlerts()
    {
        //Remove all alerts.
        docList.children.forEach {
            it.remove()
        }

        newInformation.forEach {
            //Decide whether to show the alert based on the type of information.
            val info = gameState.informations[it]!!
            if (info.tgtCharacter.contains("Anon")) return@forEach //Never show information about anonymous characters.
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

                            "Wait"
                        ).contains(info.action!!.javaClass.simpleName)
                    )//Ignore boring actions, even if they are not mine.
                    {
                        //Do nothing.
                    } else if (info.action!!.javaClass.simpleName == "Move") //If the action is a move, show the dedicated alert.
                    {
                        addAlert(
                            "moved",
                            params = arrayOf(
                                ReadOnly.prop(info.tgtCharacter),
                                ReadOnly.prop((info.action as Move).placeTo)
                            )
                        ) {
                            InformationViewUI.instance.refresh(gameState, "creationTime")
                            InformationViewUI.instance.isVisible = true
                        }
                    } else
                    {
                        //TODO: Anything else are hidden for now. Display action alerts that are important for the player.
//                        addAlert("newInfo") {
//                            InformationViewUI.instance.refresh(gameState, "creationTime")
//                            InformationViewUI.instance.isVisible = true
//                        }
                    }

                }

                InformationType.APPARATUS_DURABILITY ->
                {
                    addAlert("apparatus") {
                        ApparatusInfoUI.instance.refresh(info)
                        ApparatusInfoUI.instance.isVisible = true
                    }
                }

                else ->
                {
                    //Do nothing.
                }
            }

        }
        //Hunger and Thirst, Vitality
        if (gameState.player.hunger > ReadOnly.const("hungerThreshold"))
            addAlert("hunger")
        if (gameState.player.thirst > ReadOnly.const("thirstThreshold"))
            addAlert("thirst")
        if (gameState.player.health < ReadOnly.const("CriticalHealth"))
            addAlert("vital")
        if (gameState.player.will < ReadOnly.const("CriticalWill"))
            addAlert("will")

        isVisible = !docList.children.isEmpty
    }

    companion object
    {
        lateinit var instance: AlertUI
    }


}