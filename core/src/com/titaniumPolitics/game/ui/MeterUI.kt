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

class MeterUI : Table(), KTable {

    // Size is chosen to be used with BarSimpleFillVitals and BarSimpleBgTiledNormal.
    var cont: Container<Actor>

    var fill = 0f
    var vertical = false
    val PADDING = 25f

    init {
        debug()
        stack {
            it.fill()
            container {
                fill()
                image("BarSimpleBgTiledNormal") {

                }
            }
            this@MeterUI.cont = container {
                pad(-this@MeterUI.PADDING)
                image("BarSimpleFillVitals") {
                }
                align(Align.bottomLeft)
            }
        }
    }

    fun setValue(value: Float) {
        this.fill = value

        if (this.vertical) {
            this.cont.width
            this.cont.fill(1f, this.fill)
        } else {
            this.cont.height
            this.cont.fill(this.fill, 1f)
        }
        this.cont.layout()
    }

}