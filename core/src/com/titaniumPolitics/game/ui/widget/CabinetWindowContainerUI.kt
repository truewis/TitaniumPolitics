package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.ui.AssistantUI
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.SoundEngine
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.button
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

open class CabinetWindowContainerUI(
    val title: String,
    val content: Actor,
    val xOffset: Float,
    val yOffset: Float,
    val openAction: () -> Unit = {}
) :
    Table(Scene2DSkin.defaultSkin), KTable {
    val onClose = ArrayList<() -> Unit>()
    var isOpen = false
        private set

    val buttonWidth = 180f
    val buttonHeight = 540f
    val titleLabel: Label

    init {
        name = title
        stack {
            it.fill()
            container(
                image("cabinetHandleLight2") {
                }) {
                size(this@CabinetWindowContainerUI.buttonWidth, this@CabinetWindowContainerUI.buttonHeight)
                setTouchable(Touchable.disabled)//Want to touch the text, through the image.
            }
        }
        val text = scene2d.container(
            label(title, "docTitle") {
                setFontScale(0.4f)
                color = Color.DARK_GRAY
                setAlignment(Align.left)
            }
        ) {
            fill()
            size(100f, 30f)
        }
        addActor(text)
        titleLabel = text.actor as Label
        text.setPosition(160f, 140f)
        text.isTransform = true // Enable transformations for the text actor
        text.rotateBy(90f)
        val UI = scene2d.stack {
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
                            this@CabinetWindowContainerUI.onClose.forEach { it() }
                            this@CabinetWindowContainerUI.changeOpenState(false)
                        }
                    })
                }

                row()
                add(this@CabinetWindowContainerUI.content).colspan(2).grow()
            }
        }
        addActor(UI)
        UI.setSize(CapsuleStage.instance.width, CapsuleStage.instance.height)
        UI.setPosition(-CapsuleStage.instance.width, -yOffset)
        UI.layout()

        text.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {

                //Open content UI
                changeOpenState(true)
                changeMarkedState(false)
            }
        }
        )


    }

    fun changeOpenState(open: Boolean) {
        isOpen = open
        val otherCabinets = (parent as AssistantUI).cabinetWindowUIs
            .filter { it != this } // Exclude the current cabinet
        //Add actions animations accordingly
        actions.clear()
        otherCabinets.forEach { it.actions.clear() } // Close other cabinets
        if (open) {
            openAction() // Call the open action if provided
            addAction(
                Actions.moveTo(
                    stage.width, //When open, move to the right side of the screen. It should not depend on the parent actor's x offset.
                    y,
                    0.8f,
                    Interpolation.fade
                )
            )
            otherCabinets.forEach {
                it.addAction(
                    Actions.moveTo(
                        -200f, //When closed, move to the left side of the screen to hide the handle. It should not depend on the parent actor's x offset.
                        it.y,
                        0.8f,
                        Interpolation.fade
                    )
                )
            } // Close other cabinets
            SoundEngine.playSound("metal-drawer-open.mp3")
        } else {
            addAction(
                Actions.moveTo(
                    xOffset,
                    y,
                    0.8f,
                    Interpolation.fade
                )
            )
            otherCabinets.forEach {
                it.addAction(
                    Actions.moveTo(
                        it.xOffset, //When closed, move to the left side of the screen to hide the handle. It should not depend on the parent actor's x offset.
                        it.y,
                        0.8f,
                        Interpolation.fade
                    )
                )

            }
            SoundEngine.playSound("metal-drawer-close.mp3")
        }
    }

    fun changeMarkedState(marked: Boolean) {
        if (marked) {
            titleLabel.color = Color.GREEN
            val marker = scene2d.image("BadgeRound") {
                name = "GreenMarker"
                color = Color.GREEN
                setSize(25f, 25f)

            }
            marker.addAction(
                Actions.forever(
                    Actions.sequence(
                        Actions.delay(0.5f),
                        Actions.alpha(0f, 0.2f),
                        Actions.alpha(1f, 0.2f)
                    )
                )
            )
            addActor(marker) //Add the marker to the actor.
            marker.setPosition(120f, 90f)
        } else {
            titleLabel.color = Color.DARK_GRAY
            val marker = findActor<Image>("GreenMarker")
            marker?.remove()
        }
    }


}