package com.truewisWorkshop.titaniumPolitics.ui.widget

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.button
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.scene2d

class DivisionSelectButtonGroup(callback: (String) -> Unit) : Table(Scene2DSkin.defaultSkin), KTable {
    init {
        val buttonGroup = ButtonGroup<Button>()
        buttonGroup.setMinCheckCount(0)
        buttonGroup.setMaxCheckCount(1)
        listOf(
            "infrastructure",
            "interior",
            "safety",
            "bioengineering",
            "mining",
            "education",
            "industry"
        ).forEach { tobj ->
            val t = scene2d.button {
                //TODO:Agenda Tooltip addListener(ActionTooltipUI(tobj))
                container {
                    it.size(150f)
                    it.fill(0.66f, 0.66f)
                    it.align(Align.center)
                    image("Help") {
                        this.setDrawable(Scene2DSkin.defaultSkin, tobj + "Division")

                    }
                }
                this@button.addListener(object : ClickListener() {
                    override fun clicked(
                        event: InputEvent?,
                        x: Float,
                        y: Float
                    ) {
                        callback(tobj)
                    }
                })
            }
            add(t).size(150f).fill()
            buttonGroup.add(t)
        }
    }
}
