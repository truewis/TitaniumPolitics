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


class ResourceTransferUI(
    var gameState: GameState, actionCallback: (GameAction) -> Unit,
) :
    ActionSheetUI("ResourceTransferTitle", gameState, actionCallback) {
    private val dataTable = ResourceDisplayUI()
    private val targetTable = ResourceDisplayUI()
    private val resourceSelectTable = ResourceSelectTable(100.0, 0.0, 1.0) {}
    private val sbjCharObj = gameState.characters[subject]!!

    //Determines if the transfer is official or not.
    var mode: String = "official"

    val current get() = if (mode == "private") sbjCharObj.resources else gameState.places[tgtPlace]!!.resources
    val target = Resources()
    var toWhere = ""
        set(value) {
            field = value
            refresh(mode)
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
                    label(ReadOnly.prop("ResourceTransferUI-Transaction"), "docTitle") {
                        setFontScale(0.5f); color = Color.BLACK
                    }
                row()
                label(ReadOnly.prop("ResourceTransferUI-TransferResourcesTo"), "docTitle") {
                    setFontScale(0.5f); color = Color.BLACK
                }
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
        mode: String = this.mode
    ) {
        this.mode = mode
        this.modeLabel.setText("Transaction: $mode")
        placeButton.isVisible = mode != "private"
        dataTable.current = current
        dataTable.callback = { resourceName, amount ->
            resourceSelectTable.refresh(
                current[resourceName],
                target[resourceName],
                gameState.places[toWhere]?.workplaceParty?.let {
                    it.standardBudget.value[it.name]!![resourceName] / it.workplace.workHoursLength * ReadOnly.constInt(
                        "quarterInDays"
                    )
                }
                    ?: 0.0,
                resourceName,
                this
            )
        }
        dataTable.refresh()

        targetTable.current = target
        targetTable.callback = { resourceName, amount ->
            resourceSelectTable.refresh(
                current[resourceName],
                amount,
                gameState.places[toWhere]?.workplaceParty?.let {
                    it.standardBudget.value[it.name]!![resourceName] / it.workplace.workHoursLength * ReadOnly.constInt(
                        "quarterInDays"
                    )
                }
                    ?: 0.0, resourceName, this)
        }

        targetTable.refresh()


    }

    private fun refreshAction() {
        action = if (this@ResourceTransferUI.mode == "official") {
            OfficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjCharObj.place.name,
                this@ResourceTransferUI.toWhere,
                this@ResourceTransferUI.target,
                gameState
            )
        } else if (this@ResourceTransferUI.mode == "unofficial") {
            UnofficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjCharObj.place.name,
                toWhere = this@ResourceTransferUI.toWhere,
                false,
                this@ResourceTransferUI.target,
                gameState
            )
        } else if (this@ResourceTransferUI.mode == "private") {
            UnofficialResourceTransfer(
                this@ResourceTransferUI.subject,
                this@ResourceTransferUI.sbjCharObj.place.name,
                this@ResourceTransferUI.toWhere,
                true,
                this@ResourceTransferUI.target,
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
                addListener(object : ChangeListener() {
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

        fun refresh(
            maxAmount: Double,
            currentAmount: Double,
            hourlyConsumption: Double,
            resName: String,
            owner: ResourceTransferUI
        ) {
            isVisible = true
            slider.removeListener(slider.listeners.firstOrNull { it is ChangeListener })
            slider.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (Resources.isDiscreteResource(resName)) {
                        this@ResourceSelectTable.label.setText(
                            ReadOnly.prop("ResourceTransferUI-amountSelectedInt")
                                .format(
                                    slider.value.toInt(),
                                    if (hourlyConsumption == 0.0) 0.0 else slider.value / hourlyConsumption
                                )
                        )
                        owner.target[resName] = slider.value.toInt().toDouble()
                    } else {
                        this@ResourceSelectTable.label.setText(
                            ReadOnly.prop("ResourceTransferUI-amountSelected")
                                .format(
                                    slider.value,
                                    if (hourlyConsumption == 0.0) 0.0 else slider.value / hourlyConsumption
                                )
                        )
                        owner.target[resName] = slider.value.toDouble()
                    }
                    owner.refreshAction()
                    owner.refresh()
                }

            })
            if (Resources.isDiscreteResource(resName)) {
                slider.stepSize = 1f
                slider.value = currentAmount.toInt().toFloat()
                slider.setRange(0f, maxAmount.toInt().toFloat())
                label.setText(
                    ReadOnly.prop("ResourceTransferUI-amountSelectedInt")
                        .format(
                            currentAmount.toInt(),
                            if (hourlyConsumption == 0.0) 0.0 else currentAmount / hourlyConsumption
                        )
                )
            } else {
                slider.stepSize = maxAmount.toFloat() / 100f
                slider.value = currentAmount.toFloat()
                slider.setRange(0f, maxAmount.toFloat())
                label.setText(
                    ReadOnly.prop("ResourceTransferUI-amountSelected")
                        .format(currentAmount, if (hourlyConsumption == 0.0) 0.0 else currentAmount / hourlyConsumption)
                )
            }

        }
    }

}
