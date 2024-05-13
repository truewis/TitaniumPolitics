package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.CapsuleStage
import ktx.scene2d.*

open class MapUI(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{
    val currentConnections = arrayListOf<Connection>()
    val currentMarkers = arrayListOf<PlaceMarker>()
    val currentPlaceMarkerWindow = PlaceMarkerWindowUI(gameState, this)


    init
    {
        instance = this
        isVisible = false

        stack { cell ->
            cell.grow()
            table {
                image("panel") {
                    it.grow()
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            //Close Place Marker UI
                            this@MapUI.currentPlaceMarkerWindow.isVisible = false
                        }
                    }
                    )
                }
            }

            container {
                align(Align.topRight)
                button {
                    image("close-square-line-icon") {
                        it.size(50f)
                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@MapUI.isVisible = false
                        }
                    })
                }
            }


        }
        currentPlaceMarkerWindow.isVisible = false
        this.addActor(currentPlaceMarkerWindow)


    }


    open fun refresh()
    {
        //TODO: Home Marker, Current Location Marker
        //CurrentPlaceMarkerWindow is a window that shows up when a place marker is clicked. It should be removed and re-added to the stage to ensure it is on top.
        removeActor(currentPlaceMarkerWindow)
        currentConnections.forEach { removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        gameState.places.forEach { (placeName, place) ->
            if (!placeName.contains("home"))
            {
                place.connectedPlaces.forEach { connection ->
                    if (!connection.contains("home"))
                    {
                        addActor(Connection(gameState, placeName, connection).also {
                            it.color = Color.RED
                            currentConnections.add(it)
                        })
                    }
                }
            }
        }
        currentMarkers.forEach { removeActor(it) }
        currentMarkers.clear()
        //Draw markers for places
        gameState.places.forEach { (placeName, _) ->
            if (!placeName.contains("home"))
            {
                addActor(PlaceMarker(gameState, this, placeName).also {
                    currentMarkers.add(it)
                })
            } else if (placeName == "home_" + gameState.playerName)
            {
                addActor(HomePlaceMarker(gameState, this, placeName).also {
                    currentMarkers.add(it)
                })
            }
        }
        addActor(currentPlaceMarkerWindow)
    }

    fun convertToScreenCoords(x: Float, y: Float): Pair<Float, Float>
    {
        //Converts coordinates to screen coordinates. The Screen in centered at the player's location.
        var rel_x = x - gameState.player.place.coordinates.x
        var rel_y = y - gameState.player.place.coordinates.z
        if (gameState.player.place.name == "home_" + gameState.playerName) //Homes does not have coordinates, so we use the place the player is living by.
        {
            rel_x = x - gameState.places[gameState.player.livingBy]!!.coordinates.x
            rel_y = y - gameState.places[gameState.player.livingBy]!!.coordinates.z
        }
        val MAX_X = 20
        val MAX_Y = 15
        val MIN_X = -20
        val MIN_Y = -15
        val PADDING = 0.1
        return Pair(
            (stage.width * PADDING + (stage.width * (1 - 2 * PADDING) * (x - MIN_X) / (MAX_X - MIN_X))).toFloat(), //We don't do relative coordinates for now. We can replace x with rel_x
            (stage.height * PADDING + (stage.height * (1 - 2 * PADDING) * (y - MIN_Y) / (MAX_Y - MIN_Y))).toFloat()
        )
    }

    companion object
    {
        lateinit var instance: MapUI
    }

}