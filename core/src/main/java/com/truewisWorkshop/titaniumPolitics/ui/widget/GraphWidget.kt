package com.titaniumPolitics.game.ui.widget

import com.truewisWorkshop.titaniumPolitics.ui.GraphScreen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin

/**
 * A reusable embeddable graph widget that wraps [GraphScreen] for use inside tables
 * without the full [com.titaniumPolitics.game.ui.GraphInfoUI] window chrome.
 */
class GraphWidget(
    initialData: Map<Int, Float> = mapOf(0 to 0f),
    yDataType: GraphScreen.DataType = GraphScreen.DataType.COUNT
) : Table(defaultSkin), KTable {

    private val graphScreen = GraphScreen(initialData, yDataType)

    init {
        add(graphScreen).grow()
    }

    fun refresh(data: Map<Int, Float>, yDataType: GraphScreen.DataType = graphScreen.yDataType) {
        graphScreen.refresh(data, yDataType)
    }

    fun addHorizontalLine(yValue: Float, color: Color = Color.WHITE, thickness: Float = 2f, key: String) {
        graphScreen.addHorizontalLine(yValue, color, thickness, key)
    }

    fun removeHorizontalLine(key: String) {
        graphScreen.removeHorizontalLine(key)
    }

    fun addVerticalLine(xValue: Float, color: Color = Color.WHITE, thickness: Float = 2f, key: String) {
        graphScreen.addVerticalLine(xValue, color, thickness, key)
    }

    fun removeVerticalLine(key: String) {
        graphScreen.removeVerticalLine(key)
    }
}
