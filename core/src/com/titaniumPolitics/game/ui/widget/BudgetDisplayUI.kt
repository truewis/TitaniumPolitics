package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Budget
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
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

/**
 * A UI component that displays the budget of a party or a given budget in a scrollable table format.
 * If a party is provided, it displays the party's budget; otherwise, it displays the provided budget.
 * If the party's budget is not resolved, it shows a message indicating that the budget is not resolved.
 */
class BudgetDisplayUI(party: Party? = null, budget: Budget? = null) :
    Table(defaultSkin), KTable {
    val labelList = arrayListOf<ResourceDisplayUI>()
    val docTable = scene2d.table { }

    init {
        add(ScrollPane(docTable).also { it.setScrollingDisabled(true, false) }).grow()
        party?.let { refresh(it) } ?: budget?.let { refresh(it) }
    }

    fun refresh(_party: Party) {
        docTable.clear()
        with(docTable) {
            if (_party.isBudgetResolved)
                this@BudgetDisplayUI.refresh(_party.budget)
            else label(ReadOnly.prop("BudgetDisplayUI-notResolved"), "docTitle") {
                setAlignment(Align.center)
                setFontScale(0.4f)
            }
        }
    }

    fun refresh(_budget: Budget) {
        docTable.clear()
        with(docTable) {
            _budget.value.forEach { (sectorName, resourceAmount) ->
                table {
                    it.grow()
                    label(sectorName, "docTitle") {
                        setAlignment(Align.topLeft)
                        setFontScale(0.3f)
                    }
                    this@BudgetDisplayUI.labelList.add(ResourceDisplayUI(resourceAmount))
                }
                row()

            }
        }
    }
}
