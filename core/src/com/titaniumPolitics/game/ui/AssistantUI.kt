package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.map.MapUI
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.widget.CabinetWindowUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AssistantUI(gameState: GameState) : Table(defaultSkin) {

    val cabinetWindowUIs = mutableListOf<CabinetWindowUI>()

    init {
        val buttonWidth = 180f
        val buttonHeight = 540f
        val buttonGap = 70f
        val buttonXGap = -7f
        padLeft(-20f)

        val MapButton = scene2d.table {
            name = "MapButton"
            stack {
                it.fill()
                container(
                    image("cabinetHandleLight2") {
                    }) {
                    size(buttonWidth, buttonHeight)
                }
            }
            val text = scene2d.container(
                label("MAP", "docTitle") {
                    setFontScale(0.4f)
                    color = Color.DARK_GRAY
                    setAlignment(Align.left)
                }
            ) {
                fill()
                size(100f, 30f)
            }
            addActor(text)
            text.setPosition(160f, 140f)
            text.isTransform = true // Enable transformations for the text actor
            text.rotateBy(90f)
            val UI = CabinetWindowUI(this@table, MapUI(gameState), 0f)
            addActor(UI)
            UI.setSize(CapsuleStage.instance.width, CapsuleStage.instance.height)
            UI.setPosition(-CapsuleStage.instance.width, 0f)
            UI.layout()
            cabinetWindowUIs.add(UI) // Add the MapUI to the list of CabinetWindowUIs

            text.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {

                    //Open Map UI
                    UI.changeOpenState(!UI.isOpen)
                }
            }
            )
        }
        addActor(MapButton)
        MapButton.setSize(buttonWidth, buttonHeight + 10f)
        MapButton.setPosition(0f, 0f)

        val InformationButton = scene2d.table {
            name = "InformationButton"
            stack {
                it.fill()
                container(
                    image("cabinetHandleLight2") {
                    }) {
                    size(buttonWidth, buttonHeight)
                }
            }
            val text = scene2d.container(
                label("INFORMATION", "docTitle") {
                    setFontScale(0.4f)
                    color = Color.DARK_GRAY
                    setAlignment(Align.left)
                }
            ) {
                fill()
                size(100f, 30f)
            }
            addActor(text)
            text.setPosition(160f, 140f)
            text.isTransform = true // Enable transformations for the text actor
            text.rotateBy(90f)
            val UI = CabinetWindowUI(this@table, InformationViewUI(gameState), buttonXGap)
            addActor(UI)
            UI.setSize(CapsuleStage.instance.width, CapsuleStage.instance.height)
            UI.setPosition(-CapsuleStage.instance.width, -buttonGap)
            UI.layout()
            cabinetWindowUIs.add(UI) // Add the MapUI to the list of CabinetWindowUIs

            text.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {

                    //Open Map UI
                    UI.changeOpenState(!UI.isOpen)
                }
            }
            )
        }
        addActor(InformationButton)
        InformationButton.setSize(buttonWidth, buttonHeight + 10f)
        InformationButton.setPosition(buttonXGap, buttonGap)
        val calendarLabel = Label("0", defaultSkin, "trnsprtConsole").also {
            it.setFontScale(2f)
            it.color = Color.BLACK
            it.setAlignment(Align.center, Align.center)
        }
        val CalendarButton = scene2d.table {
            name = "CalendarButton"
            stack {
                it.fill()
                container(
                    image("cabinetHandleLight2") {
                    }) {
                    size(buttonWidth, buttonHeight)
                }
            }
            val text = scene2d.container(
                label("CALENDAR", "docTitle") {
                    setFontScale(0.4f)
                    color = Color.DARK_GRAY
                    setAlignment(Align.left)
                }
            ) {
                fill()
                size(100f, 30f)
            }
            addActor(text)
            text.setPosition(160f, 140f)
            text.isTransform = true // Enable transformations for the text actor
            text.rotateBy(90f)
            val UI = CabinetWindowUI(this@table, CalendarUI(gameState), 2 * buttonXGap)
            addActor(UI)
            UI.setSize(CapsuleStage.instance.width, CapsuleStage.instance.height)
            UI.setPosition(-CapsuleStage.instance.width, -2 * buttonGap)
            UI.layout()
            cabinetWindowUIs.add(UI) // Add the MapUI to the list of CabinetWindowUIs

            text.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {

                    //Open Map UI
                    UI.changeOpenState(!UI.isOpen)
                }
            }
            )
        }
        addActor(CalendarButton)
        CalendarButton.setSize(buttonWidth, buttonHeight + 10f)
        CalendarButton.setPosition(2 * buttonXGap, 2 * buttonGap)

        val politiciansInfoButton = scene2d.table {
            name = "politiciansInfoButton"
            stack {
                it.fill()
                container(
                    image("cabinetHandleLight2") {
                    }) {
                    size(buttonWidth, buttonHeight)
                }

            }
            val text = scene2d.container(
                label("POLITICS", "docTitle") {
                    setFontScale(0.4f)
                    color = Color.DARK_GRAY
                    setAlignment(Align.left)
                }
            ) {
                fill()
                size(100f, 30f)
            }
            addActor(text)
            text.setPosition(160f, 140f)
            text.isTransform = true // Enable transformations for the text actor
            text.rotateBy(90f)
            val UI = CabinetWindowUI(this@table, PoliticiansInfoUI(gameState), 3 * buttonXGap)
            addActor(UI)
            UI.setSize(CapsuleStage.instance.width, CapsuleStage.instance.height)
            UI.setPosition(-CapsuleStage.instance.width, -3 * buttonGap)
            UI.layout()
            cabinetWindowUIs.add(UI) // Add the MapUI to the list of CabinetWindowUIs

            text.addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {

                    //Open Map UI
                    UI.changeOpenState(!UI.isOpen)
                }
            }
            )
        }
        addActor(politiciansInfoButton)
        politiciansInfoButton.setSize(buttonWidth, buttonHeight + 10f)
        politiciansInfoButton.setPosition(3 * buttonXGap, 3 * buttonGap)

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

    fun changeColorRecursively(actor: Actor, color: Color) {
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
        changeColorRecursively(actor, Color.GREEN)
        actor.children.forEach { child ->
            child.color = Color.GREEN
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
            color = Color.GREEN
            setSize(25f, 25f)
            setPosition(actor.width * 0.75f - width / 2, actor.height * 0.75f - height / 2)
        }
        actor.addActor(marker) //Add the marker to the actor.
    }

    //unmark the button, change the color to white.
    fun unmarkButton(actor: Group) {
        changeColorRecursively(actor, Color.WHITE)
        actor.children.forEach { child ->
            child.color = Color.WHITE
        }
        actor.clearActions()
        actor.findActor<Actor>("GreenMarker_${actor.name}")?.remove() //Remove the marker if it exists.

    }


}
