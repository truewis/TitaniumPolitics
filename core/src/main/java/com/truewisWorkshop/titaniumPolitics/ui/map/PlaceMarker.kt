package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.TasksUI
import ktx.actors.alpha
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

open class PlaceMarker(val gameState: GameState, val owner: MapUI, val place: String) : Button() {
    val onClick = arrayListOf<(String) -> Unit>()
    val buildingList = gameState.places.filter {
        it.value.isBuildingIn == place
    }
    val buildingsAvailableToPlayerList = buildingList

        .filter {
            it.value.isAuthorized(gameState.playerName)
        }
    val buildingPlaceMarkers = mutableListOf<HomePlaceMarker>()
    val buildingsInThisPlaceUI = scene2d.stack {
        isTransform = true
        setOrigin(Align.center)
        val RADIUS = SIZE * 3f
        container(
            image("BadgeRound") {
                setColor(1f, 1f, 1f, 0.3f) // Semi-transparent white
            }) {
            size(RADIUS * 2)
            fill()
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    onClick.forEach { it(place) }
                    //Open Place UI
                    owner.currentPlaceMarkerWindow.refresh(place)
                }
            })
        }
        table {
            add().fill()
            //List all buildings in this place.
            buildingsAvailableToPlayerList.forEach {
                //AddActor PlaceMarker for each building in a circle around the place marker.
                    buildingEntry ->
                val buildingMarker = HomePlaceMarker(gameState, owner, buildingEntry.key)
                buildingMarker.setSize(SIZE * 0.6f, SIZE * 0.6f)
                addActor(buildingMarker)
                buildingPlaceMarkers.add(buildingMarker)
                //Set position to that building markers are distributed evenly in a circle around the place marker.
                val index = buildingsAvailableToPlayerList.keys.indexOf(buildingEntry.key)
                val total = buildingsAvailableToPlayerList.size
                val angle = index * (360f / total)
                val radian = Math.toRadians(angle.toDouble())
                val x = (RADIUS - SIZE / 2 + RADIUS * 0.8 * Math.cos(radian)).toFloat()
                val y = (RADIUS - SIZE / 2 + RADIUS * 0.8 * Math.sin(radian)).toFloat()
                buildingMarker.setPosition(x, y, Align.center)
            }
        }
    }

    init {
        //Set style of the button to the default skin.
        style = defaultSkin.get("diamond", ButtonStyle::class.java)
        val start: Pair<Float, Float> = owner.convertToScreenCoords(
            gameState.places[place]!!.coordinates.x.toFloat(),
            gameState.places[place]!!.coordinates.z.toFloat()
        )
        //Set marker to different color if player is here.
        if (gameState.player.place.name == place) {
            color = Color.RED
            addAction(
                Actions.forever(
                    Actions.sequence(
                        Actions.delay(0.5f),
                        AlphaAction().apply {
                            duration = 0.2f
                            alpha = 0f
                        },
                        AlphaAction().apply {
                            duration = 0.2f
                            alpha = 1f
                        }
                    )))
            val infoText = scene2d.label(ReadOnly.prop("PlaceMarker-YouAreHere"), "docTitle") {
                setFontScale(0.2f)
                setAlignment(Align.center)
                color = Color.RED
                touchable = Touchable.disabled // Make the label not interactable as it will cover other markers
            }
            addActor(infoText)
            infoText.setPosition(
                SIZE / 2,
                -10f, // Position it below the marker
                Align.center
            )
        }
        owner.dataTable.addActor(this)

        //Set the size of the connection to the length of the line.
        this.setSize(SIZE, SIZE)
        //Set the position of the connection to the start of the line.
        this.setPosition(start.first - width / 2, start.second - height / 2)
        this.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(
                event: ChangeEvent?,
                actor: Actor?
            ) {
                if (isBuildingVisible)// If buildings are already visible, do nothing.
                    return
                onClick.forEach { it(place) }
                //Open Place UI
                owner.currentPlaceMarkerWindow.refresh(place)
                if (buildingsAvailableToPlayerList.isNotEmpty())
                    isBuildingVisible = true
            }
        })

        if (buildingsAvailableToPlayerList.isNotEmpty()) {
            addActor(buildingsInThisPlaceUI)
            buildingsInThisPlaceUI.setPosition(width / 2, height / 2, Align.center)
            buildingsInThisPlaceUI.isVisible = false
        }


    }

    val questMarkers = mutableListOf<TasksUI.QuestMarker>()
    fun addQuestMarker(questMarker: TasksUI.QuestMarker) {
        //Only add if not already present.
        if (questMarkers.none {
                it.quest.index == questMarker.quest.index
            }) {
            questMarkers.add(questMarker)
            addActor(questMarker)
        }
    }

    override fun act(delta: Float) {
        super.act(delta)
        //If buildings are invisible, display all the building quest markers along with the place quest markers.
        //Reposition all quest markers
        if (!isBuildingVisible) {
            questMarkers.forEachIndexed { index, marker ->
                marker.setPosition(
                    this.width + (index + 1) * (SIZE),
                    this.height,
                    Align.topLeft
                )
                //Markers are not touchable when buildings are not visible, because they might overlap with building markers.
                //But if there are no buildings, they should be touchable.
                if (buildingsAvailableToPlayerList.isEmpty())
                    marker.touchable = Touchable.enabled
                else
                    marker.touchable = Touchable.disabled
            }
        } else {
            //If buildings are visible, display the building quest markers on the concentric circle around the place marker, next to their respective buildings.
            buildingsAvailableToPlayerList.forEach { buildingEntry ->
                val buildingMarker = buildingPlaceMarkers.first {
                    it.place == buildingEntry.key
                }
                val buildingQuestMarkers = questMarkers.filter {
                    it.quest.relatedPlace == buildingEntry.key
                }
                buildingQuestMarkers.forEachIndexed { index, marker ->
                    marker.setPosition(
                        buildingMarker.x + buildingsInThisPlaceUI.x/*Relative coordinates*/ + (index + 1) * (SIZE),
                        buildingMarker.y + buildingsInThisPlaceUI.y/*Relative coordinates*/,
                        Align.center
                    )
                    //Markers are touchable when buildings are visible.
                    marker.touchable = Touchable.enabled
                }
            }

        }
    }

    var isBuildingVisible = false
        set(value) {
            field = value
            isDisabled = value
            if (value) {
                buildingsInThisPlaceUI.addAction(
                    Actions.sequence(
                        Actions.run {
                            buildingsInThisPlaceUI.isVisible = true
                            buildingsInThisPlaceUI.alpha = 0f // Start from invisible
                            buildingsInThisPlaceUI.setScale(0.1f)
                        },
                        Actions.parallel(
                            Actions.fadeIn(0.3f),
                            Actions.scaleTo(1f, 1f, 0.3f)
                        )
                    )
                )
            } else {
                buildingsInThisPlaceUI.addAction(
                    Actions.sequence(
                        Actions.parallel(
                            Actions.fadeOut(0.3f),
                            Actions.scaleTo(0.1f, 0.1f, 0.3f)
                        ),
                        Actions.run {
                            buildingsInThisPlaceUI.isVisible = false
                        }
                    )
                )
            }
        }


    companion object {
        const val SIZE = 30f // Size of the marker
    }


}
