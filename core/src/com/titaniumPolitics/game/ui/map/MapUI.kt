package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.*

class MapUI(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{
    val currentConnections = arrayListOf<Connection>()
    val currentMarkers = arrayListOf<PlaceMarker>()
    val currentPlaceMarkerWindow = PlaceMarkerWindowUI()


    init
    {
        isVisible = false
        instance = this

        stack { cell ->
            cell.grow()
            table {
                image("capsuleDevLabel1") {
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
                align(Align.bottomLeft)

                //This button hase to be identical in appearance to the one in AssistantUI.
                button {
                    label("MAP", "console") {
                        setFontScale(4f)
                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            //Close Map UI
                            instance.isVisible = false
                        }
                    }
                    )
                }
            }
            //A button to hide this UI on the bottom left of the screen.


        }
        currentPlaceMarkerWindow.isVisible = false
        addActor(currentPlaceMarkerWindow)


    }


    fun refresh()
    {
        //CurrentPlaceMarkerWindow is a window that shows up when a place marker is clicked. It should be removed and re-added to the stage to ensure it is on top.
        removeActor(currentPlaceMarkerWindow)
        currentConnections.forEach { removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        gameState.places.forEach { (placeName, place) ->
            place.connectedPlaces.forEach { connection ->
                addActor(Connection(gameState, placeName, connection).also {
                    it.color = Color.RED
                    currentConnections.add(it)
                })
            }
        }
        currentMarkers.forEach { removeActor(it) }
        currentMarkers.clear()
        //Draw markers for places
        gameState.places.forEach { (placeName, _) ->
            addActor(PlaceMarker(gameState, placeName).also {
                currentMarkers.add(it)
            })
        }
        addActor(currentPlaceMarkerWindow)
    }

    companion object
    {
        //Singleton
        lateinit var instance: MapUI

        fun convertToScreenCoords(x: Float, y: Float): Pair<Float, Float>
        {
            val MAX_X = 20
            val MAX_Y = 15
            val MIN_X = -20
            val MIN_Y = -15
            val PADDING = 0.1
            return Pair(
                (Gdx.graphics.width * PADDING + (Gdx.graphics.width * (1 - 2 * PADDING) * (x - MIN_X) / (MAX_X - MIN_X))).toFloat(),
                (Gdx.graphics.height * PADDING + (Gdx.graphics.height * (1 - 2 * PADDING) * (y - MIN_Y) / (MAX_Y - MIN_Y))).toFloat()
            )
        }
    }

}