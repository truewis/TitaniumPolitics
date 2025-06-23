package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.AssistantUI
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.button
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

open class CabinetWindowUI(val parentActor: Group, val content: Actor, val xOffset: Float) :
    Table(Scene2DSkin.defaultSkin), KTable {
    val onClose = ArrayList<() -> Unit>()
    var isOpen = false
        private set

    init {
        stack {
            it.grow()
            image("GradientBottom") {
                color = Color.BLACK
            }
            image("BackgroundNoiseHD")
            table {
                add().growX()
                button {
                    it.fill()
                    it.size(70f)
                    image("XGrunge")
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@CabinetWindowUI.onClose.forEach { it() }
                            this@CabinetWindowUI.changeOpenState(false)
                        }
                    })
                }

                row()
                add(this@CabinetWindowUI.content).colspan(2).grow()
                debug()
            }
        }


    }

    fun changeOpenState(open: Boolean) {
        isOpen = open
        val otherCabinets = (parentActor.parent as AssistantUI).cabinetWindowUIs
            .filter { it != this } // Exclude the current cabinet
        //Add actions animations accordingly
        parentActor.actions.clear()
        otherCabinets.forEach { it.actions.clear() } // Close other cabinets
        if (open) {
            parentActor.addAction(
                Actions.moveTo(
                    stage.width, //When open, move to the right side of the screen. It should not depend on the parent actor's x offset.
                    parentActor.y,
                    0.5f
                )
            )
            otherCabinets.forEach {
                it.parentActor.addAction(
                    Actions.moveTo(
                        -200f, //When closed, move to the left side of the screen to hide the handle. It should not depend on the parent actor's x offset.
                        it.parentActor.y,
                        0.5f
                    )
                )
            } // Close other cabinets
        } else {
            parentActor.addAction(
                Actions.moveTo(
                    xOffset,
                    parentActor.y,
                    0.5f
                )
            )
            otherCabinets.forEach {
                it.parentActor.addAction(
                    Actions.moveTo(
                        it.xOffset, //When closed, move to the left side of the screen to hide the handle. It should not depend on the parent actor's x offset.
                        it.parentActor.y,
                        0.5f
                    )
                )
            }
        }
    }


}