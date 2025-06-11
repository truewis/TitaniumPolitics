package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.QuestUI
import com.titaniumPolitics.game.ui.WindowUI
import ktx.scene2d.*

open class MapUI(val gameState: GameState) : WindowUI("MapTitle") {
    val currentConnections = arrayListOf<Connection>()
    val currentMarkers = arrayListOf<PlaceMarker>()
    val currentPlaceMarkerWindow = PlaceMarkerWindowUI(gameState, this)
    private lateinit var scrollPane: ScrollPane
    val dataTable = Table(skin)
    val PADDING = 100f
    val WIDTH = 1920f
    val HEIGHT = 1080f * 5
    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0


    init {
        isVisible = false
        scrollPane = ScrollPane(dataTable)
        scrollPane.setScrollingDisabled(false, false)
        content.add(scrollPane).grow()


        val st = scene2d.stack {
            setSize(this@MapUI.WIDTH, this@MapUI.HEIGHT)
            name = "background"
            image("MapGrid") {
                addListener(object : ClickListener() {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        //Close Place Marker UI
                        this@MapUI.currentPlaceMarkerWindow.isVisible = false
                    }
                }
                )
            }
        }
        dataTable.addActor(st)
        st.setPosition(PADDING, PADDING)
        currentPlaceMarkerWindow.isVisible = false
        dataTable.addActor(currentPlaceMarkerWindow)
        dataTable.add().grow()


    }


    open fun refresh() {
        //Calculate the bounds.
        minX = gameState.places.minOf { it.value.coordinates.x }
        minY = gameState.places.minOf { it.value.coordinates.z }
        maxX = gameState.places.maxOf { it.value.coordinates.x }
        maxY = gameState.places.maxOf { it.value.coordinates.z }

        //Background size is determined by the extent of the markers.
        dataTable.cells[0].size(
            WIDTH + PADDING * 2, //Add some padding
            HEIGHT + PADDING * 2 //Add some padding
        )
        dataTable.pack()
        //CurrentPlaceMarkerWindow is a window that shows up when a place marker is clicked. It should be removed and re-added to the stage to ensure it is on top.
        dataTable.removeActor(currentPlaceMarkerWindow)
        currentConnections.forEach { dataTable.removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        gameState.places.forEach { (placeName, place) ->
            if (!placeName.contains("home")) {
                place.connectedPlaces.forEach { connection ->
                    if (!connection.contains("home")) {
                        Connection(gameState, this, placeName, connection).also {
                            it.color = Color.RED
                            currentConnections.add(it)
                        }
                    }
                }
            }
        }
        currentMarkers.forEach { dataTable.removeActor(it) }
        currentMarkers.clear()
        //Draw markers for places
        gameState.places.forEach { (placeName, _) ->
            if (!placeName.contains("home")) {
                PlaceMarker(gameState, this, placeName).also {
                    currentMarkers.add(it)
                }
            } else if (placeName == "home_" + gameState.playerName) {
                HomePlaceMarker(gameState, this, placeName).also {
                    currentMarkers.add(it)
                }
            }
        }
        dataTable.addActor(currentPlaceMarkerWindow)

        //Add quest markers.
        gameState.eventSystem.quests.forEach { quest ->
            if (quest.tgtPlace != null) {
                QuestUI.QuestMarker(quest).also { marker ->
                    setSize(50f, 50f)
                    dataTable.addActor(marker)
                    val coords = currentMarkers.first { it.place == quest.tgtPlace }
                    marker.setPosition(
                        coords.x,
                        coords.y
                    )
                }
            }
        }

        //Scroll to the player's place.
        val playerPlaceMarker = currentMarkers.first { it.place == gameState.player.place.name }

        scrollPane.scrollTo(
            playerPlaceMarker.x - scrollPane.width / 2,
            playerPlaceMarker.y + scrollPane.height / 2,
            scrollPane.width,
            scrollPane.height
        )

    }

    fun convertToScreenCoords(x: Float, y: Float): Pair<Float, Float> {
        //Converts coordinates to screen coordinates. The Screen in centered at the player's location.
        var rel_x = x - gameState.player.place.coordinates.x
        var rel_y = y - gameState.player.place.coordinates.z
        if (gameState.player.place.name == "home_" + gameState.playerName) //Homes does not have coordinates, so we use the place the player is living by.
        {
            rel_x = x - gameState.places[gameState.player.livingBy]!!.coordinates.x
            rel_y = y - gameState.places[gameState.player.livingBy]!!.coordinates.z
        }
        return Pair(
            PADDING + (dataTable.width - 2 * PADDING) * (x - minX) / (maxX - minX), //We do absolute coordinates for now. We can replace x with rel_x.
            PADDING + (dataTable.height - 2 * PADDING) * (y - minY) / (maxY - minY)
        )
    }

    //DO not make this class singleton, as it is used in multiple places such as PlaceSelectionUI.

}