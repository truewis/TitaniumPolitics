package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AssistantUI(gameState: GameState) : Table(defaultSkin), KTable {

    init {
        val buttonSize = 100f
        button {
            name = "politiciansInfoButton"
            it.fill()
            it.size(buttonSize)
            stack {
                it.size(50f)
                it.fill()
                image("CrownGrunge") {
                }
            }
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    this@AssistantUI.findActor<Group>("politiciansInfoButton")?.also {
                        this@AssistantUI.unmarkButton(it)
                    }
                    if (PoliticiansInfoUI.instance.isVisible) this@AssistantUI.closeAll()
                    else {
                        PoliticiansInfoUI.instance.isVisible = !PoliticiansInfoUI.instance.isVisible
                        PoliticiansInfoUI.instance.refresh()
                    }
                }
            }
            )
        }
        row()
        button {
            name = "InformationButton"
            it.fill()
            it.size(buttonSize)
            stack {
                it.size(50f)
                it.fill()
                image("icon_app_140") {
                }
            }
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    this@AssistantUI.findActor<Group>("InformationButton")?.also {
                        this@AssistantUI.unmarkButton(it)
                    }
                    if (InformationViewUI.instance.isVisible) this@AssistantUI.closeAll()
                    else {
                        InformationViewUI.instance.isVisible = !InformationViewUI.instance.isVisible
                        InformationViewUI.instance.refresh("creationTime")
                    }
                }
            }
            )
        }
        row()
        val calendarLabel = Label("0", defaultSkin, "trnsprtConsole").also {
            it.setFontScale(3f)
            it.setAlignment(Align.center, Align.center)
        }
        button {
            name = "CalendarButton"
            it.fill()
            it.size(buttonSize)
            stack {
                it.size(50f)
                it.fill()
                image("icon_app_119") {
                }
                add(calendarLabel)
            }
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    this@AssistantUI.findActor<Group>("CalendarButton")?.also {
                        this@AssistantUI.unmarkButton(it)
                    }
                    //Open Calendar UI
                    if (HeadUpInterface.instance.calendarUI.isVisible) this@AssistantUI.closeAll()
                    else {
                        HeadUpInterface.instance.calendarUI.refresh(gameState)
                        HeadUpInterface.instance.calendarUI.isVisible = !HeadUpInterface.instance.calendarUI.isVisible
                    }
                }
            }
            )
        }
        add(ClockUI(gameState)).growX().align(Align.left)

        row()

        button {
            name = "MapButton"
            it.fill()
            it.size(buttonSize)
            image("icon_app_195") {
                it.size(50f)
            }
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {

                    //Open Map UI
                    if (HeadUpInterface.instance.mapUI.isVisible) this@AssistantUI.closeAll()
                    else {
                        HeadUpInterface.instance.mapUI.refresh()
                        HeadUpInterface.instance.mapUI.isVisible = !HeadUpInterface.instance.mapUI.isVisible
                    }
                }
            }
            )
        }
        add(PlaceAndCoordUI(gameState)).growX()


        gameState.timeChanged += { _, y ->
            Gdx.app.postRunnable {

                calendarLabel.setText((gameState.day).toString()) // Current Date.

            }
        }

        //Mark the calendar button When new meeting is scheduled within the next 5 days.
        //Also check CalendarUI
        gameState.onAddScheduledMeeting += { meeting ->
            Gdx.app.postRunnable {
                if (meeting.time - gameState.day * 86400 / ReadOnly.dt <= 5 * 86400 / ReadOnly.dt && meeting.scheduledCharacters.contains(
                        gameState.playerName
                    )
                ) {//5 days from today's start
                    markButton(findActor("CalendarButton"))
                }
            }
        }
    }

    fun changeColorRecursively(actor: Actor, color: com.badlogic.gdx.graphics.Color) {
        actor.color = color
        if (actor is Group) {
            actor.children.forEach { child ->
                changeColorRecursively(child, color)
            }
        }
    }

    //Change the color to green, display green marker, and blink the button.
    fun markButton(actor: Group) {

        if (actor.findActor<Actor>("GreenMarker_${actor.name}") != null)
            return //If the marker already exists, do not mark again.
        changeColorRecursively(actor, com.badlogic.gdx.graphics.Color.GREEN)
        actor.children.forEach { child ->
            child.color = com.badlogic.gdx.graphics.Color.GREEN
        }

        actor.addAction(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(0f, 0.5f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(1f, 0.5f)
                )
            )
        )
        val marker = scene2d.image("BadgeRound") {
            name = "GreenMarker_${actor.name}"
            color = com.badlogic.gdx.graphics.Color.GREEN
            setSize(25f, 25f)
            setPosition(actor.width * 0.75f - width / 2, actor.height * 0.75f - height / 2)
        }
        actor.addActor(marker) //Add the marker to the actor.
    }

    //unmark the button, change the color to white.
    fun unmarkButton(actor: Group) {
        changeColorRecursively(actor, com.badlogic.gdx.graphics.Color.WHITE)
        actor.children.forEach { child ->
            child.color = com.badlogic.gdx.graphics.Color.WHITE
        }
        actor.clearActions()
        actor.findActor<Actor>("GreenMarker_${actor.name}")?.remove() //Remove the marker if it exists.

    }

    fun closeAll() {
        InformationViewUI.instance.isVisible = false
        HeadUpInterface.instance.mapUI.isVisible = false
        HeadUpInterface.instance.calendarUI.isVisible = false
        HeadUpInterface.instance.politiciansInfoUI.isVisible = false
        PlaceSelectionUI.instance.isVisible = false
        CharacterSelectUI.instance.isVisible = false
        ResourceInfoUI.instance.isVisible = false
        ApparatusInfoUI.instance.isVisible = false
        HumanResourceInfoUI.instance.isVisible = false
        ResourceTransferUI.primary.isVisible = false
        NewAgendaUI.primary.isVisible = false
    }


}
