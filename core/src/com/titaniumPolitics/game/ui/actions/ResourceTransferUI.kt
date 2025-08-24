package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.OfficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import ktx.scene2d.*


class ResourceTransferUI(var gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("ResourceTransferTitle", gameState, actionCallback) {
    private val dataTable = ResourceDisplayUI()
    private val targetTable = ResourceDisplayUI()
    override var tgtPlace: String = gameState.player.place.name
        set(value) {
            field = value
            refresh(mode, gameState.places[value]!!.resources.toHashMap(), target)
        }
    private val sbjChar = gameState.characters[subject]!!

    //Determines if the transfer is official or not.
    var mode: String = "official"
    var current = hashMapOf<String, Double>()
    var target = hashMapOf<String, Double>()
    var toWhere = ""
    var modeLabel: Label
    var placeButton: Button
    var action: GameAction? = null

    init {
        val st = stack {
            it.grow()
            table {
                this@ResourceTransferUI.modeLabel =
                    label("Transaction", "docTitle") { setFontScale(0.5f); color = Color.BLACK }
                row()
                label("Transfer resources to", "docTitle") { setFontScale(0.5f); color = Color.BLACK }
                //Select place to transfer resources to.
                this@ResourceTransferUI.placeButton = PlaceSelectButton({
                    this@ResourceTransferUI.toWhere = it
                })
                add(this@ResourceTransferUI.placeButton).size(400f, 75f)

                row()
                add(this@ResourceTransferUI.dataTable)
                add(this@ResourceTransferUI.targetTable)
                row()
                add(this@ResourceTransferUI.submitButton).size(400f, 75f)
                    .fill().colspan(2)
            }
        }
        content.add(st).grow()


    }


    fun refresh(
        mode: String,
        current: HashMap<String, Double> = hashMapOf(),
        target: HashMap<String, Double> = hashMapOf(),
    ) {
        this.current = current
        this.target = target
        this.mode = mode
        this.modeLabel.setText("Transaction: $mode")
        placeButton.isVisible = mode != "private"
        dataTable.current = Resources(current)
        dataTable.callback = { resourceName, amount ->
            current[resourceName] = current[resourceName]!! - 1
            target[resourceName] = (target[resourceName] ?: .0) + 1
            refreshAction()
            this@ResourceTransferUI.refresh(mode, current, target)
        }
        dataTable.refresh()

        targetTable.current = Resources(target)
        targetTable.callback = { resourceName, amount ->
            target[resourceName] = target[resourceName]!! - 1
            current[resourceName] = (current[resourceName] ?: .0) + 1
            refreshAction()
            this@ResourceTransferUI.refresh(mode, current, target)
        }

        targetTable.refresh()


    }

    private fun refreshAction() {
        action = if (this@ResourceTransferUI.mode == "official") {
            OfficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjChar.place.name
            ).apply {
                this.resources = Resources(this@ResourceTransferUI.target)
                this.toWhere = this@ResourceTransferUI.toWhere
            }
        } else if (this@ResourceTransferUI.mode == "unofficial") {
            UnofficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjChar.place.name
            ).apply {
                this.resources = Resources(this@ResourceTransferUI.target)
                this.toWhere = this@ResourceTransferUI.toWhere
            }
        } else if (this@ResourceTransferUI.mode == "private") {
            UnofficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjChar.place.name
            ).apply {
                this.resources = Resources(this@ResourceTransferUI.target)
                this.toWhere = this@ResourceTransferUI.toWhere
                this.fromHome = true
            }
        } else {
            throw IllegalArgumentException("Invalid mode: $mode")
        }
        action!!.injectParent(gameState)
        submitButton.refresh(action!!)
    }

}