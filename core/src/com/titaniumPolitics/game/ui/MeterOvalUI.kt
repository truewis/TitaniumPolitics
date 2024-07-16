package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.Align
import com.ray3k.tenpatch.TenPatchDrawable
import ktx.scene2d.*

class MeterOvalUI : Table(), KTable
{
    var cont: Cell<*>
    var tab: Table
    var fillIm = scene2d.image("SliderFill") {

    }

    var fill = 0f
    var vertical = false

    init
    {
        stack {
            container {
                size(300f, 30f)
                image("SliderBg") {
                    color = com.badlogic.gdx.graphics.Color(0.5f, 0.5f, 0.5f, 0.7f)
                }

            }

            this@MeterOvalUI.tab = table {
                table {
                    this@MeterOvalUI.cont = it
                    clip = true
                    addActor(this@MeterOvalUI.fillIm)
                }

            }
            container {
                size(5f, 30f)
                image("Stroke5pxVertical") {
                    color = Color.WHITE
                }
            }
        }
    }

    fun setValue(value: Float)
    {
        width = 300f
        height = 30f
        fill = value
        fillIm.color = if (fill > 0.5f) com.badlogic.gdx.graphics.Color(
            0f,
            1f,
            0f,
            1f
        ) else com.badlogic.gdx.graphics.Color(1f, 0f, 0f, 1f)
        fillIm.setSize(300f, 30f)
        if (fill > 0.5f)
            fillIm.setPosition(-150f, 0f)
        else
            fillIm.setPosition(-300f * fill, 0f)

        if (!vertical)
        {
            if (fill > 0.5f)
            {
                cont.size(width * (fill - 0.5f), height).pad(0f, width * 0.5f, 0f, width * (1f - fill))
            } else
            {
                cont.size(width * (0.5f - fill), height).pad(0f, width * fill, 0f, width * 0.5f)
            }

        } else
        {
            if (fill > 0.5f)
            {
                cont.size(width, height * (fill - 0.5f)).pad(height * 0.5f, 0f, height * (1f - fill), 0f)
            } else
            {
                cont.size(width, height * (0.5f - fill)).pad(height * fill, 0f, height * 0.5f, 0f)
            }
        }
        //tab.debug()
        tab.layout()
    }

}