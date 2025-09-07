package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.InformationSourceUI
import com.titaniumPolitics.game.ui.widget.MeterUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*

class ApparatusPanelUI(var info: Information) : Button(Scene2DSkin.defaultSkin), KTable {

    init {
        stack {
            debug()
            it.grow()
            image("GradientBottom") {
                color = Color.BLACK
            }
            image("BackgroundNoiseHD")


            table {
                image("CogGrunge") {
                    it.size(120f).fill()
                    try {
                        drawable = TextureRegionDrawable(
                            CapsuleStage.Companion.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                                ReadOnly.appJson[this@ApparatusPanelUI.info.tgtApparatusName!!]!!.jsonObject["image"]!!.jsonPrimitive.content,
                                Texture::class.java
                            )!!
                        )
                    } catch (e: Exception) {
                        Logger.write(
                            "Portrait Image Error: ${this@ApparatusPanelUI.info.tgtApparatusName!!}",
                            Logger.LogLevel.INFO
                        )
                    }
                }
                table {
                    it.growX().fillY()
                    label(ReadOnly.appProp(this@ApparatusPanelUI.info.tgtApparatusName!!), "docTitle") {
                        setFontScale(0.4f)
                    }
                    row()
                    container(MeterUI().apply {
                        setValue((this@ApparatusPanelUI.info.variables["durability"]!! / 100.0).toFloat())
                        color = Color.GREEN
                    }) {
                        it.growX().fillY()
                        padTop(5f)
                        size(200f, 20f)
                        fill()
                    }
                    row()
                    container(MeterUI().apply {
                        setValue((this@ApparatusPanelUI.info.variables["efficiency"]!! / 100.0).toFloat())
                        color = Color.BLUE
                    }) {
                        it.growX().fillY()
                        padTop(5f)
                        size(200f, 20f)
                        fill()
                    }
                    row()
                    label(
                        "Danger: %.1f %%/100HR".format(this@ApparatusPanelUI.info.variables["danger"]!! * 3600 * 100 * 100/*percent*/),
                        "docTitle"
                    ) {
                        it.growX().fillY()
                        setFontScale(0.2f)
                        if (this@ApparatusPanelUI.info.variables["danger"]!! * 3600 * 100 * 100 > 1)
                            color = Color.RED
                        else
                            color = Color.WHITE
                    }
                    row()
                    add(InformationSourceUI(this@ApparatusPanelUI.info)).growX().fillY()
                }
            }
        }
    }


}