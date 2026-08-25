package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.Scene2DSkin.defaultSkin

class AlertUI(var gameState: GameState) : Table(defaultSkin) {
    private val docList = VerticalGroup()
    private var newInformation = hashSetOf<String>()

    init {
        instance = this
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.onAddInfo += { it -> if (it.knownTo.contains(gameState.playerName)) newInformation.add(it.name) }
        gameState.updateUI += { _ -> displayAlerts(); }
        gameState.onPlayerAction += {
            //Remove all alerts.
            Gdx.app.postRunnable {
                docList.clear()
            }
        }
    }

    fun addAlert(type: String, vararg params: String, action: () -> Unit = {}) {
        Gdx.app.postRunnable {//This function is often called from the main thread, so we need to post it to the UI thread.
            val singletonTypes = listOf("vital", "hunger", "thirst", "will", "ppsFlammableGas", "ppsCorrosiveGas", "ppsGasLeak", "ppsReactionRisk")
            if (type in singletonTypes && docList.children.none {
                    (it as AlertPanelUI).type == type
                })//Only one alert of each type is visible at a time.
                docList.addActor(AlertPanelUI(type, action, docList, *params))
            else if (type !in singletonTypes)
                docList.addActor(AlertPanelUI(type, action, docList, *params))
            if (!isVisible)
                isVisible = true
        }
    }

    fun displayAlerts() {
        val missedMeetingAlertsIterator = gameState.missedMeetingAlerts.iterator()
        while (missedMeetingAlertsIterator.hasNext()) {
            val meetingId = missedMeetingAlertsIterator.next()
            missedMeetingAlertsIterator.remove()
            addAlert("meetingCanceled", meetingId) {
                AssistantUI.instance.calendarButton.changeOpenState(true)
            }
        }

        newInformation.forEach {
            //Decide whether to show the alert based on the type of information.
            val info = gameState.informations[it]!!
            if (info.tgtCharacter == null || info.tgtCharacter !in gameState.knownCharactersToPlayer
            ) return@forEach //Never show information about unknown characters to the player.
            when (info.type) {
                InformationType.ACCIDENT -> {
                    addAlert("accident", ReadOnly.placeProp(info.tgtPlace), gameState.formatTime()) {
                        AssistantUI.instance.informationButton.changeOpenState(true)
                    }
                }

                InformationType.CASUALTY -> {
                    addAlert(
                        "casualty",
                        params = arrayOf(
                            info.amount.toString(),
                            ReadOnly.placeProp(info.tgtPlace)
                        )
                    ) {
                        AssistantUI.instance.informationButton.changeOpenState(true)
                    }
                }

                InformationType.ACTION -> {
                    if (info.tgtCharacter == gameState.playerName//Ignore my actions, they are not surprising.
                        || info.action is Wait
                    )//Ignore boring actions, even if they are not mine.
                    {
                        //Do nothing.
                    } else if (info.action is Move) //If the action is a move, show the dedicated alert.
                    {
                        addAlert(
                            "moved",
                            params = arrayOf(
                                ReadOnly.charProp(info.tgtCharacter ?: "Someone"),
                                ReadOnly.placeProp((info.action as Move).placeTo)
                            )
                        ) {
                            AssistantUI.instance.informationButton.changeOpenState(true)
                        }
                    } else {
                        //TODO: Anything else are hidden for now. Display action alerts that are important for the player.
//                        addAlert("newInfo") {
//                            InformationViewUI.instance.refresh(gameState, "creationTime")
//                            InformationViewUI.instance.isVisible = true
//                        }
                    }

                }

                InformationType.APPARATUS -> {
                    addAlert("apparatus") {
                        ApparatusInfoUI.instance.display(info)
                    }
                }

                else -> {
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

        //PPS gas hazard alerts for the four gas dangers
        val playerPlace = try { gameState.player.place } catch (e: Exception) { null }
        playerPlace?.let { place ->
            //1. Flammable gas (increases fire danger) alert
            place.gasResources.keys.forEach { gasName ->
                val gasData = ReadOnly.gasJson[gasName]?.jsonObject
                if (gasData?.get("fireHazard")?.jsonPrimitive?.boolean == true) {
                    val pressure = try { place.gasPressure(gasName).toFloat() } catch (e: Exception) { 0f }
                    if (pressure > 500f)
                        addAlert("ppsFlammableGas", gasName)
                }
            }
            //2. Corrosive gas (damages apparatus) alert
            place.gasResources.keys.forEach { gasName ->
                val gasData = ReadOnly.gasJson[gasName]?.jsonObject
                if (gasData?.get("corrosive")?.jsonPrimitive?.boolean == true) {
                    val pressure = try { place.gasPressure(gasName).toFloat() } catch (e: Exception) { 0f }
                    if (pressure > 100f)
                        addAlert("ppsCorrosiveGas", gasName)
                }
            }
            //3. Gas leak alert (damaged apparatus leaking)
            place.apparatuses.forEach { app ->
                val leakGasName = ReadOnly.appJson[app.name]?.jsonObject?.get("leakGas")?.jsonPrimitive?.content
                if (leakGasName != null && app.durability < ReadOnly.const("DurabilityMax") * 0.5 && app.durability > 0.0)
                    addAlert("ppsGasLeak", leakGasName, app.name)
            }
            //4. Gas reaction risk (high methane concentration)
            val methanePressure = try { place.gasPressure("methane").toFloat() } catch (e: Exception) { 0f }
            if (methanePressure > ReadOnly.const("MethaneReactionThresholdPa").toFloat() * 0.5f)
                addAlert("ppsReactionRisk")
        }

        newInformation.clear()

    }

    companion object {
        lateinit var instance: AlertUI
    }


}
