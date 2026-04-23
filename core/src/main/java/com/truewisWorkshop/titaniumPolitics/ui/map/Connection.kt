package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Place
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.math.atan2
import kotlin.math.sqrt

class Connection(var gameState: GameState, val owner: MapUI, startPlace: String, endPlace: String) : Image() {
    init {
        touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        owner.dataTable.addActor(this)
        //Fetch default drawable from skin.
        drawable = defaultSkin.getDrawable("icon_simpleshape_10")

        // Determine corridor properties from whichever endpoint is a corridor.
        val corridorPlace: Place? = sequenceOf(startPlace, endPlace)
            .mapNotNull { if (it.contains("corridor")) gameState.places[it] else null }
            .firstOrNull()

        // Width: proportional to corridor radius (manwayI=2m→5f, manwayII=4m→10f, manwayIII=6m→15f).
        val corridorRadius = corridorPlace?.corridorRadius
        val lineWidth = if (corridorRadius != null) (corridorRadius / 2.0 * 5.0).toFloat() else 5f

        // Color: based on transport methods present in the corridor.
        val transportTypes = corridorPlace?.apparatuses
            ?.filter { it.isTransportInfrastructure && it.transportType != "manway" && it.transportType != "pressurizer" }
            ?.mapNotNull { it.transportType }
            ?.toSet()
            ?: emptySet()
        color = when {
            "railway" in transportTypes -> Color(0.9f, 0.4f, 0.1f, 0.8f)       // warm orange
            "elevatorCrane" in transportTypes -> Color(0.2f, 0.4f, 0.9f, 0.8f) // steel blue
            "cartPath" in transportTypes -> Color(0.6f, 0.4f, 0.2f, 0.8f)      // brown
            "liquidPipe" in transportTypes || "gasPipe" in transportTypes ->
                Color(0.2f, 0.7f, 0.6f, 0.8f)                                  // teal
            "powerLine" in transportTypes -> Color(0.9f, 0.8f, 0.1f, 0.8f)     // yellow
            else -> Color(0f, 0f, 0f, 0.5f)                                     // default black
        }

        try {
            val start: Pair<Float, Float> = owner.convertToScreenCoords(
                gameState.places[startPlace]!!.coordinates.x.toFloat(),
                gameState.places[startPlace]!!.coordinates.z.toFloat()
            )
            val end: Pair<Float, Float> = owner.convertToScreenCoords(
                gameState.places[endPlace]!!.coordinates.x.toFloat(),
                gameState.places[endPlace]!!.coordinates.z.toFloat()
            )

            //Set the position of the connection to the start of the line.
            setPosition(start.first, start.second)
            //Set the size of the connection to the length of the line.
            setSize(
                sqrt((end.first - start.first) * (end.first - start.first) + (end.second - start.second) * (end.second - start.second)),
                lineWidth
            )
            //Set the rotation of the connection to the angle of the line.
            rotation =
                Math.toDegrees(atan2((end.second - start.second).toDouble(), (end.first - start.first).toDouble()))
                    .toFloat()
        } catch (e: Exception) {
            Gdx.app.log("Connection", "Error: $startPlace, $endPlace. $e")
        }

    }


}
