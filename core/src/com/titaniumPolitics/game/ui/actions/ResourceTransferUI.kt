package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
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
    private val resourceSelectTable = ResourceSelectTable(100.0, 0.0, 1.0) {}
    override var tgtPlace: String = gameState.player.place.name
        set(value) {
            field = value
            refresh(mode, gameState.places[value]!!.resources.toHashMap(), hashMapOf())
            refreshAction()
        }
    private val sbjChar = gameState.characters[subject]!!

    //Determines if the transfer is official or not.
    var mode: String = "official"
    var current = hashMapOf<String, Double>()
    var target = hashMapOf<String, Double>()
    var toWhere = ""
        set(value) {
            field = value
            refresh(mode, gameState.places[tgtPlace]!!.resources.toHashMap(), hashMapOf())
            refreshAction()
        }
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
                add(this@ResourceTransferUI.resourceSelectTable).size(400f, 75f)
                    .fill().colspan(2)
                row()
                this@ResourceTransferUI.resourceSelectTable.isVisible = false
                add(this@ResourceTransferUI.submitButton).size(400f, 75f)
                    .fill().colspan(2)
            }
        }
        content.add(st).grow()


    }


    fun refresh(
        mode: String,
        _current: HashMap<String, Double> = this.current,
        _target: HashMap<String, Double> = this.target,
    ) {
        this.current = _current
        this.target = _target
        this.mode = mode
        this.modeLabel.setText("Transaction: $mode")
        placeButton.isVisible = mode != "private"
        dataTable.current = Resources(current)
        dataTable.callback = { resourceName, amount ->
            resourceSelectTable.refresh(
                current[resourceName]!!,
                target[resourceName] ?: 0.0,
                gameState.places[toWhere]?.workplaceParty?.let {
                    it.standardBudget.value[it.name]!![resourceName] / it.workplace.workHoursLength * ReadOnly.constInt(
                        "quarterInDays"
                    )
                }
                    ?: 0.0
            ) { selectedAmount ->
                target[resourceName] = selectedAmount
                refreshAction()
                this@ResourceTransferUI.refresh(mode)
            }
        }
        dataTable.refresh()

        targetTable.current = Resources(target)
        targetTable.callback = { resourceName, amount ->
            resourceSelectTable.refresh(
                current[resourceName]!!,
                amount,
                gameState.places[toWhere]?.workplaceParty?.let {
                    it.standardBudget.value[it.name]!![resourceName] / it.workplace.workHoursLength * ReadOnly.constInt(
                        "quarterInDays"
                    )
                }
                    ?: 0.0) { selectedAmount ->
                target[resourceName] = selectedAmount
                refreshAction()
                this@ResourceTransferUI.refresh(mode)
            }
        }

        targetTable.refresh()


    }

    private fun refreshAction() {
        action = if (this@ResourceTransferUI.mode == "official") {
            OfficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjChar.place.name,
                this@ResourceTransferUI.toWhere,
                Resources(this@ResourceTransferUI.target),
                gameState
            )
        } else if (this@ResourceTransferUI.mode == "unofficial") {
            UnofficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjChar.place.name,
                toWhere = this@ResourceTransferUI.toWhere,
                false,
                Resources(this@ResourceTransferUI.target),
                gameState
            )
        } else if (this@ResourceTransferUI.mode == "private") {
            UnofficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjChar.place.name,
                this@ResourceTransferUI.toWhere,
                true,
                Resources(this@ResourceTransferUI.target),
                gameState
            )
        } else {
            throw IllegalArgumentException("Invalid mode: $mode")
        }
        action!!.injectParent(gameState)
        submitButton.refresh(action!!)
    }

    class ResourceSelectTable(
        maxAmount: Double,
        currentAmount: Double,
        hourlyConsumption: Double,
        callback: (Double) -> Unit = {}
    ) :
        Table(Scene2DSkin.defaultSkin), KTable {
        val slider: Slider
        val label = scene2d.label(
            ReadOnly.prop("ResourceTransferUI-amountSelected")
                .format(currentAmount, currentAmount / hourlyConsumption), "docTitle"
        ) {
            setFontScale(0.3f)
        }

        init {
            slider = slider {
                it.width(300f)
                value = currentAmount.toFloat()
                stepSize = maxAmount.toFloat() / 100f
                setRange(0f, maxAmount.toFloat())
                addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        this@ResourceSelectTable.label.setText(
                            ReadOnly.prop("ResourceTransferUI-amountSelected")
                                .format(value, currentAmount / hourlyConsumption)
                        )
                        callback(value.toDouble())
                    }

                })
            }
            row()
            add(label)
        }

        fun refresh(maxAmount: Double, currentAmount: Double, hourlyConsumption: Double, callback: (Double) -> Unit) {
            isVisible = true
            slider.value = currentAmount.toFloat()
            slider.stepSize = maxAmount.toFloat() / 100f
            slider.setRange(0f, maxAmount.toFloat())
            label.setText(
                ReadOnly.prop("ResourceTransferUI-amountSelected")
                    .format(currentAmount, if (hourlyConsumption == 0.0) 0.0 else currentAmount / hourlyConsumption)
            )
            slider.removeListener(slider.listeners.first { it is ChangeListener })
            slider.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@ResourceSelectTable.label.setText(
                        ReadOnly.prop("ResourceTransferUI-amountSelected")
                            .format(
                                slider.value,
                                if (hourlyConsumption == 0.0) 0.0 else slider.value / hourlyConsumption
                            )
                    )
                    callback(slider.value.toDouble())
                }

            })
        }
    }

}