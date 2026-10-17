package com.titaniumPolitics.game.ui.actions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Tooltip
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.PolicyCatalog
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.ChangePolicy
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.*

class ChangePolicyUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("ChangePolicyTitle", gameState, actionCallback) {

    private val categoryTabTable = scene2d.buttonGroup(1, 1)
    private val policyListTable = Table()
    private val policyListPane = ScrollPane(policyListTable)

    private val party
        get() = gameState.player.currentMeeting?.involvedParty?.let { gameState.parties[it] }

    init {
        policyListPane.setScrollingDisabled(true, false)

        val categoryTabPane = ScrollPane(categoryTabTable)
        categoryTabPane.setScrollingDisabled(false, true)

        val st = stack {
            it.grow()
            table {
                add(categoryTabPane).fillX().height(80f)
                row()
                add(this@ChangePolicyUI.policyListPane).grow()
                row()
                add(this@ChangePolicyUI.submitButton)
            }
        }
        content.add(st).grow()

        refreshCategories()
    }

    private fun refreshCategories() {
        categoryTabTable.clear()
        val firstCategory = ChangePolicy.POLICY_CATEGORIES.keys.first()
        ChangePolicy.POLICY_CATEGORIES.keys.forEach { category ->
            categoryTabTable.button("check") {
                isChecked = category == firstCategory
                label(ReadOnly.prop("policy-category-$category"), "docTitle") {
                    color = Color.WHITE
                    setFontScale(0.3f)
                    setAlignment(Align.center)
                }
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        this@ChangePolicyUI.refreshPolicies(category)
                    }
                })
            }.inCell.size(160f, 60f)
        }
        refreshPolicies(firstCategory)
    }

    private fun affectedCharacters(policyName: String): Pair<List<String>, List<String>> {
        return PolicyCatalog.affectedCharacters(party, policyName)
    }

    private fun refreshPolicies(category: String) {
        policyListTable.clear()
        val policies = ChangePolicy.POLICY_CATEGORIES[category] ?: return
        val currentPolicies = party?.policies ?: emptySet<String>()

        policyListTable.add(buttonGroup(0, 1) {
            policies.forEach { policyName ->
                button("check") {
                    it.fillX().height(80f).pad(2f)
                    isChecked = policyName in currentPolicies
                    isDisabled = isChecked
                    label(ReadOnly.prop("policy-$policyName"), "docTitle") {
                        it.growX().padLeft(10f)
                        color = Color.WHITE
                        setFontScale(0.3f)
                        setAlignment(Align.left)
                    }

                    val (positiveList, negativeList) = this@ChangePolicyUI.affectedCharacters(policyName)
                    val tooltipTable = scene2d.table {
                        background = Scene2DSkin.defaultSkin.getDrawable("BlackPx")
                        pad(10f)
                        label(ReadOnly.prop("policy-$policyName-description"), "docTitle") {
                            it.width(280f).growX()
                            color = Color.WHITE
                            setFontScale(0.25f)
                            wrap = true
                            setAlignment(Align.topLeft)
                        }
                        if (positiveList.isNotEmpty()) {
                            row()
                            label(
                                ReadOnly.prop("policy-tooltip-positive") + ": " +
                                    positiveList.joinToString(", ") { n -> ReadOnly.charProp(n) },
                                "docTitle"
                            ) {
                                it.width(280f).growX()
                                color = Color.GREEN
                                setFontScale(0.25f)
                                wrap = true
                            }
                        }
                        if (negativeList.isNotEmpty()) {
                            row()
                            label(
                                ReadOnly.prop("policy-tooltip-negative") + ": " +
                                    negativeList.joinToString(", ") { n -> ReadOnly.charProp(n) },
                                "docTitle"
                            ) {
                                it.width(280f).growX()
                                color = Color.RED
                                setFontScale(0.25f)
                                wrap = true
                            }
                        }
                    }
                    addListener(Tooltip(tooltipTable))

                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@ChangePolicyUI.submitButton.refresh(
                                ChangePolicy(
                                    this@ChangePolicyUI.subject,
                                    this@ChangePolicyUI.tgtPlace,
                                    policyName,
                                    this@ChangePolicyUI.gameState
                                )
                            )
                        }
                    })
                }
                row()
            }
        }).fillX()
    }
}
