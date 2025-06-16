package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.stack
import ktx.scene2d.table

//This class is a UI element that displays the player's portrait and their health and will meters.
class CharStatusUI(gameState: GameState) : Table(defaultSkin), KTable {

    init {
        stack {
            container(
                image(CapsuleStage.instance.assetManager.get<Texture>("idcard_contrast.png"))
            ) {
                size(480f, 300f)
                setColor(1f, 1f, 1f, 0.8f) // Semi-transparent background
            }
            container {
                padRight(100f)
                padLeft(50f)
                padTop(40f)
                padBottom(40f)
                table {
                    label("The Triumvirate of Titanium", "docTitle") {
                        it.colspan(3)
                        it.left()
                        it.padLeft(50f)
                        setFontScale(0.2f)
                        color = Color.BLACK
                        setAlignment(com.badlogic.gdx.utils.Align.center)
                    }
                    row()
                    add(SimplePortraitUI(gameState.player.name, 0.2f, false)).size(100f, 100f)
                        .align(com.badlogic.gdx.utils.Align.center)
                    table {
                        label(gameState.player.name, "docTitle") {
                            it.left()
                            setFontScale(0.7f)
                            color = Color.BLACK
                            setAlignment(com.badlogic.gdx.utils.Align.center)
                        }
                        row()
                        label("Division", "docTitle")
                        {
                            it.left()
                            setFontScale(0.3f)
                            color = Color.BLACK
                            setAlignment(com.badlogic.gdx.utils.Align.center)
                        }
                        row()
                        label(ReadOnly.prop(gameState.player.division!!.name), "docTitle") {
                            it.left()
                            setFontScale(0.5f)
                            color = Color.BLACK
                            setAlignment(com.badlogic.gdx.utils.Align.center)
                        }
                        row()
                        add(HealthMeter(gameState)).fill()
                        row()
                        add(WillMeter(gameState)).fill()
                        row()

                    }
                }
            }

        }
    }

}
