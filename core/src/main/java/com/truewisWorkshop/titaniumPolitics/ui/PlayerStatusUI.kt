package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.HealthMeter
import com.titaniumPolitics.game.ui.widget.SimpleHeadPortraitUI
import com.titaniumPolitics.game.ui.widget.WillMeter
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import java.util.*

//This class is a UI element that displays the player's portrait and their health and will meters.
class PlayerStatusUI(gameState: GameState) : Table(defaultSkin), KTable {

    init {
        stack {
            it.right()
            container(
                image(CapsuleStage.instance.assetManager.get<Texture>("idcard_contrast.png"))
            ) {
                size(480f, 300f)
                setColor(1f, 1f, 1f, 0.75f) // Semi-transparent background
            }
            container {
                padRight(100f)
                padLeft(50f)
                padTop(40f)
                padBottom(40f)
                table {
                    label("The Triumvirate of Titanium     " + UUID.randomUUID(), "docTitle") {
                        it.colspan(3)
                        it.left()
                        it.padLeft(10f)
                        setFontScale(0.15f)
                        color = this@PlayerStatusUI.skin.getColor("BackgroundGray")
                        setAlignment(com.badlogic.gdx.utils.Align.center)
                    }
                    row()
                    table {
                        it.size(100f, 200f).top().padRight(5f).padTop(10f)
                        add(SimpleHeadPortraitUI(gameState.player.name, false)).fill().top().size(100f).expandY()
                        row()
                        label(ReadOnly.prop("PlayerStatusUI-Verified"), "docTitle") {
                            it.center()
                            //it.padLeft(2f)
                            setFontScale(0.2f)
                            color = this@PlayerStatusUI.skin.getColor("BackgroundGray")
                        }
                        row()
                        label(ReadOnly.prop("PlayerStatusUI-IDNumber"), "docTitle") {
                            it.center()
                            //it.padLeft(2f)
                            setFontScale(0.3f)
                            color = this@PlayerStatusUI.skin.getColor("BackgroundGray")
                        }

                    }

                    table {
                        label(gameState.player.name, "docTitle") {
                            it.left()
                            //it.padLeft(2f)
                            setFontScale(0.7f)
                            color = Color.BLACK
                            setAlignment(com.badlogic.gdx.utils.Align.center)
                        }
                        row()
                        label(ReadOnly.prop(gameState.player.division!!.name), "docTitle") {
                            it.left()
                            //it.padLeft(2f)
                            setFontScale(0.25f)
                            color = this@PlayerStatusUI.skin.getColor("BackgroundGray")
                            setAlignment(com.badlogic.gdx.utils.Align.center)
                        }
                        row()
                        label(ReadOnly.prop("PlayerStatusUI-Director"), "docTitle") {
                            it.left()
                            //it.padLeft(2f)
                            setFontScale(0.20f)
                            color = this@PlayerStatusUI.skin.getColor("BackgroundGray")
                            setAlignment(com.badlogic.gdx.utils.Align.center)
                        }
                        row()
                        add(HealthMeter(gameState)).fill().padTop(30f)
                        row()
                        add(WillMeter(gameState)).fill()
                        row()

                    }
                }
            }

            addListener(
                object : ClickListener() {
                    override fun clicked(
                        event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                        x: Float,
                        y: Float
                    ) {
                        if (event?.button == 0) { // Left click
                            CharacterDetailUI.instance.refresh(gameState.player)
                            CharacterDetailUI.instance.isVisible = true
                        }
                    }
                }
            )

        }
    }

}
