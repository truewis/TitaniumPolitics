package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.scenes.scene2d.ui.Window
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.label
import ktx.scene2d.scene2d

class PlaceMarkerWindowUI : Window("Place Marker", defaultSkin)
{
    var placeDisplayed = ""

    init
    {
        titleLabel.setFontScale(2f)
        setSize(300f, 200f)
        setPosition(100f, 100f)
        add(scene2d.button {
            label("Move to Place", "console") {
                setFontScale(2f)
            }
            addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    //Move to place.
                }
            })
        }).fill()
    }

    fun show(x: Float, y: Float, placeName: String)
    {
        if (placeDisplayed == placeName)
        {
            isVisible = false
            placeDisplayed = ""

        } else
        {
            val XOFFSET = 10f
            val YOFFSET = 10f
            setPosition(x + XOFFSET, y + YOFFSET)
            isVisible = true
            this.titleLabel.setText(placeName)
            placeDisplayed = placeName
        }
    }
}