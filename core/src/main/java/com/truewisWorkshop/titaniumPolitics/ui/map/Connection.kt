package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Place
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class Connection(var gameState: GameState, val owner: MapUI, startPlace: String, endPlace: String) : Image() {
    private val roadRegion: TextureRegion
    private val tileLength: Float

    // Line geometry relative to this actor's bounds, which are the line's axis-aligned bounding box.
    // Group culling ignores rotation, so the bounds must cover where the line is actually drawn.
    private var startX = 0f
    private var startY = 0f
    private var lineLength = 0f
    private var lineAngle = 0f
    private var lineThickness = 0f
    private val lanes = mutableListOf<Lane>()
    private val laneRegion: TextureRegion

    /** One stripe along the road for each transport method installed in the corridor. */
    private class Lane(val color: Color, val operational: Boolean)

    //TiledDrawable cannot be drawn rotated, so the repeated road quad is drawn manually.
    override fun draw(batch: Batch, parentAlpha: Float) {
        if (lineLength <= 0f) return
        roadRegion.u2 = lineLength / tileLength
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        batch.draw(
            roadRegion, x + startX, y + startY - lineThickness / 2, 0f, lineThickness / 2,
            lineLength, lineThickness, 1f, 1f, lineAngle
        )
        val laneWidth = lineThickness / lanes.size.coerceAtLeast(1) * 0.5f
        lanes.forEachIndexed { i, lane ->
            val centre = -lineThickness / 2 + lineThickness * (i + 0.5f) / lanes.size
            val alpha = if (lane.operational) 0.95f else 0.35f
            batch.setColor(lane.color.r, lane.color.g, lane.color.b, alpha * parentAlpha * color.a)
            batch.draw(
                laneRegion, x + startX, y + startY + centre - laneWidth / 2, 0f, laneWidth / 2 - centre,
                lineLength, laneWidth, 1f, 1f, lineAngle
            )
        }
    }

    init {
        touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled
        owner.dataTable.addActor(this)

        // Determine corridor properties from whichever endpoint is a corridor.
        val corridorPlace: Place? = sequenceOf(startPlace, endPlace)
            .mapNotNull { if (it.contains("corridor")) gameState.places[it] else null }
            .firstOrNull()

        // Width: proportional to corridor radius (manwayI=2m -> 5f, manwayII=4m -> 10f, manwayIII=6m -> 15f).
        val corridorRadius = corridorPlace?.corridorRadius
        val lineWidth = if (corridorRadius != null) (corridorRadius * WIDTH_PER_METRE).toFloat() else DEFAULT_LINE_WIDTH

        //Road texture repeats along the line; one tile spans the line width vertically.
        val roadTexture = MapArt.texture(MapArt.CORRIDOR_ROAD)
        roadTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        roadTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge)
        roadRegion = TextureRegion(roadTexture)
        tileLength = lineWidth * roadTexture.width / MapArt.CORRIDOR_ROAD_HEIGHT
        laneRegion = ktx.scene2d.Scene2DSkin.defaultSkin.getRegion("white-pixel")
        // Lanes: one coloured stripe per transport method present in the corridor. Shown once official transfer is unlocked.
        val transportApparatuses = corridorPlace?.apparatuses
            ?.filter { it.isTransportInfrastructure && it.transportType != "manway" && it.transportType != "pressurizer" }
            ?: emptyList()
        if (gameState.progression.contains("OfficialResourceTransfer")) {
            transportApparatuses.groupBy { laneKind(it.transportType!!) }.toSortedMap().forEach { (kind, list) ->
                lanes.add(Lane(laneColor(kind), list.any { it.durability > 0 }))
            }
        }
        color = Color(1f, 1f, 1f, 0.9f)
        try {
            val start: Pair<Float, Float> = owner.convertToScreenCoords(
                gameState.places[startPlace]!!.coordinates.x.toFloat(),
                gameState.places[startPlace]!!.coordinates.z.toFloat()
            )
            val end: Pair<Float, Float> = owner.convertToScreenCoords(
                gameState.places[endPlace]!!.coordinates.x.toFloat(),
                gameState.places[endPlace]!!.coordinates.z.toFloat()
            )

            lineLength = sqrt((end.first - start.first) * (end.first - start.first) + (end.second - start.second) * (end.second - start.second))
            lineThickness = lineWidth
            lineAngle = Math.toDegrees(atan2((end.second - start.second).toDouble(), (end.first - start.first).toDouble())).toFloat()
            //Bounds = bounding box of the line, padded by the thickness to cover the width of the road.
            val pad = lineWidth
            val minX = minOf(start.first, end.first) - pad
            val minY = minOf(start.second, end.second) - pad
            setBounds(minX, minY, abs(end.first - start.first) + 2 * pad, abs(end.second - start.second) + 2 * pad)
            startX = start.first - minX
            startY = start.second - minY
        } catch (e: Exception) {
            Gdx.app.log("Connection", "Error: $startPlace, $endPlace. $e")
        }

    }

    companion object {
        /** Liquid and gas pipes share one lane. */
        private fun laneKind(type: String) = if (type == "liquidPipe" || type == "gasPipe") "pipe" else type

        private fun laneColor(kind: String) = when (kind) {
            "railway" -> Color(0.9f, 0.4f, 0.1f, 1f)       // warm orange
            "elevatorCrane" -> Color(0.2f, 0.4f, 0.9f, 1f) // steel blue
            "cartPath" -> Color(0.6f, 0.4f, 0.2f, 1f)      // brown
            "pipe" -> Color(0.2f, 0.7f, 0.6f, 1f)          // teal
            "powerLine" -> Color(0.9f, 0.8f, 0.1f, 1f)     // yellow
            else -> Color(0.7f, 0.7f, 0.7f, 1f)
        }

        /** Pixels of line width per metre of corridor radius. manwayI(2m) -> 5px, manwayII(4m) -> 10px, manwayIII(6m) -> 15px. */
        private const val WIDTH_PER_METRE = 2.5

        /** Default line width used when no corridor radius information is available. */
        private const val DEFAULT_LINE_WIDTH = 5f
    }
}
