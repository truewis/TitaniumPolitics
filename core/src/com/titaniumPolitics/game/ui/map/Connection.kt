package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.titaniumPolitics.game.core.GameState

class Connection(var gameState: GameState, startPlace: String, endPlace: String) : Actor()
{
    private val shapeRenderer = ShapeRenderer()
    private val start: Pair<Float, Float> = MapUI.convertToScreenCoords(
        gameState.places[startPlace]!!.coordinates.x.toFloat(),
        gameState.places[startPlace]!!.coordinates.y.toFloat()
    )
    private val end: Pair<Float, Float> = MapUI.convertToScreenCoords(
        gameState.places[endPlace]!!.coordinates.x.toFloat(),
        gameState.places[endPlace]!!.coordinates.y.toFloat()
    )

    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch?, parentAlpha: Float)
    {
        batch?.end()


        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = color
        shapeRenderer.line(start.first, start.second, end.first, end.second)
        shapeRenderer.end()

        batch?.begin()
    }

    //Because we are using a ShapeRenderer, we need to dispose of it when we are done with it.
    fun dispose()
    {
        shapeRenderer.dispose()
    }
}