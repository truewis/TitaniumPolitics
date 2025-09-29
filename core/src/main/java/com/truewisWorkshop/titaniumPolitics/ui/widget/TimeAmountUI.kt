package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label

class TimeAmountUI(amount: Int, unknown: Boolean = false) : Table(Scene2DSkin.defaultSkin), KTable {
    init {
        image("ClockGrunge") {
            it.size(25f)
            if (amount < 0) {
                color = Color.RED
            } else {
                color = Color.WHITE
            }
        }
        if (unknown) {
            label("?", "docTitle") {
                it.fill()
                setFontScale(0.4f)
                color = Color.WHITE
            }
        } else if (amount < 0) {
            label("${ReadOnly.toMinutes(-amount)}M", "docTitle") {
                it.fill()
                setFontScale(0.4f)
                color = Color.RED
            }
        } else if (ReadOnly.toTotalMinutes(amount) < 1) {
            label("1M", "docTitle") {
                it.fill()
                setFontScale(0.4f)
                color = Color.WHITE
            }

        } else if (ReadOnly.toTotalMinutes(amount) in 60..60 * 24) {
            if (ReadOnly.toMinutes(amount) % 60 != 0) {
                label("${ReadOnly.toHours(amount)}H${ReadOnly.toMinutes(amount) % 60}M", "docTitle") {
                    it.fill()
                    setFontScale(0.3f)
                    color = Color.WHITE
                }
            } else {
                label("${ReadOnly.toHours(amount)}H", "docTitle") {
                    it.fill()
                    setFontScale(0.4f)
                    color = Color.WHITE
                }
            }
        } else if (amount * ReadOnly.DT / 60 >= 60 * 24) {
            if (ReadOnly.toTotalHours(amount) % 24 != 0) {
                label("${ReadOnly.toDays(amount)}D${ReadOnly.toHours(amount)}H", "docTitle") {
                    it.fill()
                    setFontScale(0.3f)
                    color = Color.WHITE
                }
            } else {
                label("${ReadOnly.toDays(amount)}D", "docTitle") {
                    it.fill()
                    setFontScale(0.4f)
                    color = Color.WHITE
                }
            }
        } else
            label("${ReadOnly.toMinutes(amount)}M", "docTitle") {
                it.fill()
                setFontScale(0.4f)
                color = Color.WHITE
            }
    }

}

