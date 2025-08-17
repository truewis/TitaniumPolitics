package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Stat
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class StatRadarGraph(
    val stat: Stat,
    val fillColor: Color = Color(0.2f, 0.6f, 1f, 0.5f),
) : Table(), KTable {
    val bg = Scene2DSkin.defaultSkin.getDrawable("triangleRadar")
    var radius = 100f

    // Create and place logos, ethos, and pathos labels
    val logos = scene2d.label(ReadOnly.prop("logos") + ": ${stat.logos}", "docTitle") {
        setFontScale(0.2f)
        setAlignment(Align.center)
    }
    val ethos = scene2d.label(ReadOnly.prop("ethos") + ": ${stat.ethos}", "docTitle") {
        setFontScale(0.2f)
        setAlignment(Align.center)
    }
    val pathos = scene2d.label(ReadOnly.prop("pathos") + ": ${stat.pathos}", "docTitle") {
        setFontScale(0.2f)
        setAlignment(Align.center)
    }
    val image = image("triangleRadar") {
        it.size(this@StatRadarGraph.radius) // Set a default size for the background image
        it.center()
    }

    init {
        addActor(logos)
        addActor(ethos)
        addActor(pathos)
    }

    override fun layout() {
        super.layout()
        ethos.setPosition(image.x + width / 2 - radius / 2, image.y + height / 2 - radius * 0.55f, Align.center)
        logos.setPosition(image.x + width / 2, image.y + height / 2 + radius / 2, Align.center)
        pathos.setPosition(image.x + width / 2 + radius / 2, image.y + height / 2 - radius * 0.55f, Align.center)
    }


    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        batch?.end()

        val renderer = Stage().batch as? ShapeRenderer ?: ShapeRenderer()
        renderer.projectionMatrix = stage.camera.combined
        renderer.begin(ShapeRenderer.ShapeType.Filled)

        val centerX = x + image.x + radius / 2
        val centerY = y + image.y + radius * 0.37f // Adjusted with the center of the image
        val angles = listOf(PI / 2, PI / 2 + 2 * PI / 3, PI / 2 + 4 * PI / 3)
        val scales = listOf(stat.logos / 10f, stat.ethos / 10f, stat.pathos / 10f)

        val vertices = angles.zip(scales).map { (angle, scale) ->
            val r = radius * scale / 4
            val vx = centerX + r * cos(angle).toFloat()
            val vy = centerY + r * sin(angle).toFloat()
            vx to vy
        }

        renderer.color = fillColor
        renderer.triangle(
            vertices[0].first, vertices[0].second,
            vertices[1].first, vertices[1].second,
            vertices[2].first, vertices[2].second
        )

        renderer.end()
        batch?.begin()
    }
}