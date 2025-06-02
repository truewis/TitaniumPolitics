package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.titaniumPolitics.game.core.GameState
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.math.atan2
import kotlin.math.sqrt

class Connection(var gameState: GameState, val owner: MapUI, startPlace: String, endPlace: String) : Image()
{
    init
    {
        owner.dataTable.addActor(this)
        //Fetch default drawable from skin.
        drawable = defaultSkin.getDrawable("BlackPx")
        try
        {

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
                5f
            )
            //Set the rotation of the connection to the angle of the line.
            rotation =
                Math.toDegrees(atan2((end.second - start.second).toDouble(), (end.first - start.first).toDouble()))
                    .toFloat()
        } catch (e: Exception)
        {
            Gdx.app.log("Connection", "Error: $startPlace, $endPlace. $e")
        }

    }


}