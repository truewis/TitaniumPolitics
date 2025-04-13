package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable


class ScaledTiledDrawable : TiledDrawable
{
    private val scale = Vector2()
    private val transform = Affine2()
    private val matrix = Matrix4()
    private val oldMatrix = Matrix4()

    constructor() : super()

    constructor(region: TextureRegion?) : super(region)

    constructor(drawable: TextureRegionDrawable?) : super(drawable)

    override fun getScale(): Float
    {
        return scale.x
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float)
    {
        oldMatrix.set(batch.transformMatrix)
        matrix.set(transform.setToTrnScl(x, y, scale.x, scale.y))

        batch.transformMatrix = matrix

        super.draw(batch, 0f, 0f, width / scale.x, height / scale.y)

        batch.transformMatrix = oldMatrix
    }
}