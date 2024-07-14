package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import ktx.scene2d.KTable
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.stack

class MeterUI : Table(), KTable
{
    lateinit var cont: Container<Actor>

    var fill = 0f
    var vertical = false

    init
    {
        stack {
            container {
                fillX()
                image("BarSimpleBgTiledNormal") {

                }
            }
            this@MeterUI.cont = container {
                image("BarSimpleFillVitals") {
                }
                align(Align.bottomLeft)
            }
        }
    }

    fun setValue(value: Float)
    {
        this.fill = value
        if (this.vertical)
            this.cont.fill(1f, this.fill)
        else
            this.cont.fill(this.fill, 1f)
        this.cont.layout()
    }

}