package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.scrollPane
import ktx.scene2d.stack
import ktx.scene2d.table
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.abs

/**
 * A UI component that displays resources in a scrollable table format.
 * Callback is invoked when a resource label is clicked, passing the resource name and amount.
 */
class ResourceDisplayUI(var current: Resources = Resources(), var callback: (String, Double) -> Unit = { _, _ -> }) :
    Table(defaultSkin), KTable {
    val labelList = arrayListOf<Label>()
    val docTable = scene2d.table { }

    init {
        add(ScrollPane(docTable).also { it.setScrollingDisabled(true, false) }).grow()
        refresh()
    }

    fun refresh() {
        docTable.clear()
        with(docTable) {
            this@ResourceDisplayUI.current.forEach { (resourceName, resourceAmount) ->
                if (abs(resourceAmount) > 1e-6) {
                    table {
                        it.grow()
                        val tooltip = ResourceTooltipUI(resourceName)
                        addListener(tooltip)
                        image("CogGrunge") {
                            it.size(30f)
                            it.fill()
                        }
                        this@ResourceDisplayUI.labelList.add(
                            label(
                                "%s: %.1f".format(
                                    ReadOnly.itemProp(resourceName),
                                    resourceAmount
                                ), "docTitle"
                            ) {
                                setFontScale(0.5f)
                                setAlignment(Align.left)
                                addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@ResourceDisplayUI.deselectAllLabels()
                                        this@ResourceDisplayUI.selectLabel(this@label)
                                        this@ResourceDisplayUI.callback(
                                            resourceName,
                                            this@ResourceDisplayUI.current[resourceName]
                                        )
                                    }
                                })
                            })
                    }
                    row()
                }
            }
        }
    }

    fun deselectAllLabels() {
        labelList.forEach {
            it.color = Color.WHITE
        }
    }

    fun selectLabel(label: Label) {
        label.color = Color.YELLOW
    }
}
