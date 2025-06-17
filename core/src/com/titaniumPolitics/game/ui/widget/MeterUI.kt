package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Container
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

    override fun setColor(color: Color?) {
        super.setColor(color)
        this.cont.actor.color = color ?: Color.WHITE
    }

    override fun setColor(r: Float, g: Float, b: Float, a: Float) {
        super.setColor(r, g, b, a)
        this.cont.actor.color.set(r, g, b, a)
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