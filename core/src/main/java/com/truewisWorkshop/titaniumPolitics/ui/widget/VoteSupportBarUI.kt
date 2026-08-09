package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.stack

class VoteSupportBarUI(val candidateName: String) : Table(defaultSkin), KTable {
    val bar = MeterUI()
    lateinit var nameLabel: Label
    lateinit var supportLabel: Label

    init {
        this.add(label(ReadOnly.prop(candidateName), "docTitle") {
            setFontScale(0.2f)
            setAlignment(Align.left)
            color = Color.WHITE
        }.also { nameLabel = it }).expand().fill()
        
        this.add(label("", "docTitle") {
            setFontScale(0.2f)
            setAlignment(Align.right)
            color = Color.WHITE
        }.also { supportLabel = it }).expand().fill()
        
        this.row()
        
        this.add(bar).colspan(2).expand().fill().height(40f)
        
        bar.color = Color.BLUE
    }

    fun setValue(value: Float, percentage: Float = value * 100) {
        bar.setValue(value)
        supportLabel.setText("${String.format("%.1f", percentage)}%")
    }
}
