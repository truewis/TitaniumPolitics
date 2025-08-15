package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
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
    val submitButton: Button = scene2d.button {
        isDisabled = true//Disabled until a place is selected.
        label("Transfer", "docTitle") {
            setFontScale(0.5f)
            setAlignment(Align.center)
            color = Color.BLACK
        }
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                if (this@ResourceTransferUI.mode == "official") {
                    this@ResourceTransferUI.actionCallback(
                        OfficialResourceTransfer(
                            this@ResourceTransferUI.subject,
                            this@ResourceTransferUI.sbjChar.place.name
                        ).apply {
                            this.resources = Resources(this@ResourceTransferUI.target)
                            this.toWhere = this@ResourceTransferUI.toWhere
                        }
                    )
                } else if (this@ResourceTransferUI.mode == "unofficial") {
                    this@ResourceTransferUI.actionCallback(
                        UnofficialResourceTransfer(
                            this@ResourceTransferUI.subject,
                            this@ResourceTransferUI.sbjChar.place.name
                        ).apply {
                            this.resources = Resources(this@ResourceTransferUI.target)
                            this.toWhere = this@ResourceTransferUI.toWhere
                        }
                    )
                } else if (this@ResourceTransferUI.mode == "private") {
                    this@ResourceTransferUI.actionCallback(
                        UnofficialResourceTransfer(
                            this@ResourceTransferUI.subject,
                            this@ResourceTransferUI.sbjChar.place.name
                        ).apply {
                            this.resources = Resources(this@ResourceTransferUI.target)
                            this.toWhere = this@ResourceTransferUI.toWhere
                            this.fromHome = true
                        }
                    )
                }
                this@ResourceTransferUI.onClose.forEach { it() }
            }
        })
    }

    init {
        val st = stack {
            it.grow()
            table {
                this@ResourceTransferUI.modeLabel =
                    label("Transaction", "docTitle") { setFontScale(0.5f); color = Color.BLACK }
                row()
                label("Transfer resources to", "docTitle") { setFontScale(0.5f);color = Color.BLACK }
                //Select place to transfer resources to.
                this@ResourceTransferUI.placeButton = PlaceSelectButton(skin, {
                    this@ResourceTransferUI.toWhere = it
                    this@ResourceTransferUI.submitButton.isDisabled = false
                })
                add(this@ResourceTransferUI.placeButton).size(400f, 75f)

                row()
                add(this@ResourceTransferUI.dataTable)
                add(this@ResourceTransferUI.targetTable)
                row()
                add(this@ResourceTransferUI.submitButton).size(400f, 75f)
                    .fill()//TODO: official transfer is only to my division
                button {
                    it.fill()
                    it.size(400f, 75f)
                    label("Cancel", "docTitle") {
                        setFontScale(0.5f)
                        setAlignment(Align.center)
                        color = Color.BLACK
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                            this@ResourceTransferUI.onClose.forEach { it() }
                        }
                    })
                }
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
            this@ResourceTransferUI.refresh(mode, current, target)
        }
        dataTable.refresh()

        targetTable.current = Resources(target)
        targetTable.callback = { resourceName, amount ->
            target[resourceName] = target[resourceName]!! - 1
            current[resourceName] = (current[resourceName] ?: .0) + 1
            this@ResourceTransferUI.refresh(mode, current, target)
        }

        targetTable.refresh()


    }

}