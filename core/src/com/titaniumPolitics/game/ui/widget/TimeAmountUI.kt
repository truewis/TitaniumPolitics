package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label

class TimeAmountUI(amount: Int) : Table(Scene2DSkin.defaultSkin), KTable {
    init {
        image("ClockGrunge") {
            it.size(50f)
            if (amount < 0) {
                color = Color.RED
            } else {
                color = Color.BLACK
            }
        }
        if (amount < 0) {
            label("${ReadOnly.toMinutes(-amount)}m", "description") {
                it.fill()
                setFontScale(0.4f)
                color = Color.RED
            }
        } else if (ReadOnly.toMinutes(amount) < 1) {
            label("1m", "description") {
                it.fill()
                setFontScale(0.4f)
                color = Color.BLACK
            }

        } else if (ReadOnly.toMinutes(amount) in 60..60 * 24) {
            if (ReadOnly.toMinutes(amount) % 60 != 0) {
                label("${ReadOnly.toHours(amount)}h ${ReadOnly.toMinutes(amount) % 60}m", "description") {
                    it.fill()
                    setFontScale(0.3f)
                    color = Color.BLACK
                }
            } else {
                label("${ReadOnly.toHours(amount)}h", "description") {
                    it.fill()
                    setFontScale(0.4f)
                    color = Color.BLACK
                }
            }
        } else if (amount * ReadOnly.dt / 60 >= 60 * 24) {
            if (ReadOnly.toHours(amount) % 24 != 0) {
                label("${ReadOnly.toDays(amount)}d ${ReadOnly.toHours(amount) % 24}h", "description") {
                    it.fill()
                    setFontScale(0.3f)
                    color = Color.BLACK
                }
            } else {
                label("${ReadOnly.toDays(amount)}d", "description") {
                    it.fill()
                    setFontScale(0.4f)
                    color = Color.BLACK
                }
            }
        } else
            label("${ReadOnly.toMinutes(amount)}m", "description") {
                it.fill()
                setFontScale(0.4f)
                color = Color.BLACK
            }
    }

}

