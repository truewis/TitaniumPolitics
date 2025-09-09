package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import com.titaniumPolitics.game.ui.actions.AddInfoUI
import com.titaniumPolitics.game.ui.actions.EndSpeechUI
import com.titaniumPolitics.game.ui.actions.ExamineUI
import com.titaniumPolitics.game.ui.actions.NewAgendaUI
import com.titaniumPolitics.game.ui.actions.PrepareInfoUI
import com.titaniumPolitics.game.ui.actions.RepairUI
import com.titaniumPolitics.game.ui.actions.ResourceTransferUI
import com.titaniumPolitics.game.ui.actions.WaitUI
import com.titaniumPolitics.game.ui.actions.WaitUIMode
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.ActionTooltipUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableActionsUI(var gameState: GameState) : Table(defaultSkin), KTable {
    private val docList = scene2d.buttonGroup(0, 1)

    val dateLabel: Label
    val timeLabel: Label
    val placeLabel: Label
    val actionSheetContainer = Container<ActionSheetUI>()

    init {
        gameState.updateUI += { _ ->
            refreshList();
            close()
        }

        val docScr = ScrollPane(docList)
        docList.align(Align.center)
        stack {
            it.grow()
            container(image(CapsuleStage.instance.assetManager.get<Texture>("document_small_contrast.png"))) {
                size(1000f, 1390f)
                padLeft(-55f)
                padRight(-50f)
            }
            container {
                top()
                table {
                    table {
                        background = skin.getDrawable("simpleBorder")
                        color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                        it.padTop(60f)
                        it.fill()
                        it.expandX()
                        table {
                            background = skin.getDrawable("simpleBorder")
                            color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                            it.grow()
                            it.left()
                            label("Form 28-1", "docTitle") {
                                it.left()
                                setFontScale(0.4f)
                                color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                                setAlignment(Align.left)
                            }
                            row()
                            label("Rev. Megaros 23. 4. 1.", "docTitle") {
                                it.left()
                                setFontScale(0.2f)
                                color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                                setAlignment(Align.left)
                            }
                            row()
                            label("Division of Internal Affairs", "docTitle") {
                                it.left()
                                setFontScale(0.2f)
                                color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                                setAlignment(Align.left)
                            }
                        }
                        label("Administrative Action Report", "docTitle") {
                            it.center()
                            it.fill()
                            it.expandX()
                            setFontScale(0.5f)
                            color = Color.BLACK
                            setAlignment(Align.center)
                        }
                        table {
                            background = skin.getDrawable("simpleBorder")
                            color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                            it.right()
                            it.fill()
                            it.expandX()
                            this@AvailableActionsUI.dateLabel =
                                label(this@AvailableActionsUI.gameState.formatDate(), "docTitle") {
                                    it.right()
                                    it.fill()
                                    it.expandX
                                    setFontScale(0.3f)
                                    color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                                    setAlignment(Align.right)

                                }
                            row()
                            this@AvailableActionsUI.timeLabel =
                                label(this@AvailableActionsUI.gameState.formatClock(), "docTitle") {
                                    it.right()
                                    it.fill()
                                    it.expandX
                                    setFontScale(0.3f)
                                    color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                                    setAlignment(Align.right)

                                }
                            row()
                            this@AvailableActionsUI.placeLabel =
                                label(ReadOnly.prop(this@AvailableActionsUI.gameState.player.place.name), "docTitle") {
                                    it.right()
                                    it.fill()
                                    it.expandX
                                    setFontScale(0.25f)
                                    color = this@AvailableActionsUI.skin.getColor("BackgroundGray")
                                    setAlignment(Align.right)
                                    //TODO: overflow if the place name is too long.

                                }
                        }
                    }
                    row()

                    label("Action Performed", "docTitle") {
                        it.center()
                        setFontScale(0.25f)
                        color = Color.BLACK
                        setAlignment(Align.center)
                    }
                    row()
                    add(docScr).size(900f, 150f)
                    row()
                    add(this@AvailableActionsUI.actionSheetContainer).size(900f, 700f).fill()
                    this@AvailableActionsUI.actionSheetContainer.top()
                }
            }
        }


    }

    //TODO: also make changes to NewAgendaUI.kt.
    fun refreshList() {
        docList.clear()
        dateLabel.setText(gameState.formatDate())
        timeLabel.setText(gameState.formatClock())
        placeLabel.setText(ReadOnly.placeProp(gameState.player.place.name))
        GameEngine.availableActions(
            gameState,
            gameState.player.place.name,
            gameState.playerName
        ).forEachIndexed { index, tobj ->
            //We do not create buttons for these actions, as they are accessible through the main UI.
            if (listOf("Move", "Talk").contains(tobj)) {
                return@forEachIndexed
            }
            val illagal =
                tobj == "UnofficialResourceTransfer" && gameState.player.place.whoseHome != gameState.playerName || tobj == "Resign" || tobj == "Intercept"
            val t = createActionButton(
                index,
                tobj,
                true, //Dangerous actions are those that can be persecuted by the law, such as UnofficialResourceTransfer from workplaces,  i.e. stealing resources from the workplace.
                dangerous = illagal,
                gameState,
                this::setActionSheet,
                {
                    //If time to the next schedule is less than the action duration,
                    val func = {
                        when (it::class.simpleName) {
                            "UnofficialResourceTransfer", "OfficialResourceTransfer", "InvestigateAccidentScene", "ClearAccidentScene", "Eat", "Repair", "PrepareInfo", "Examine" -> {
                                ProgressBackgroundUI.instance.setVisibleWithFade(
                                    true,
                                    it::class.simpleName!!
                                )
                            }
                        }
                        GameEngine.acquireCallback(it)
                    }

                    if (AssistantUI.instance.calendarUI.timeToNextScheduledMeeting()
                            ?.let { i -> i < it.expectedDuration }
                            ?: false
                    ) {
                        BlockingWarningUI.instance.display(
                            "notEnoughTimeUntilNextSchedule",
                            func
                        )
                    } else if (illagal) {
                        BlockingWarningUI.instance.display(
                            "illegal",
                            func
                        )
                    } else {
                        func()
                    }
                })
            t.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent, actor: Actor) {
                    if (docList.buttonGroup.checked == null) {
                        close()
                    }
                }
            })
            docList.add(t).size(100f).fill()
        }
        isVisible = !docList.children.isEmpty

    }

    fun setActionSheet(actionSheet: ActionSheetUI) {
        this.actionSheetContainer.actor = actionSheet
        actionSheet.onClose += this::close
        addAction(
            Actions.moveTo(
                this.x, //When open, move to the right side of the screen. It should not depend on the parent actor's x offset.
                350f,
                0.5f
            )
        )
    }

    fun close() {
        addAction(
            Actions.moveTo(
                this.x, //When open, move to the right side of the screen. It should not depend on the parent actor's x offset.
                -350f,
                0.5f
            )
        )
        this.actionSheetContainer.actor = null
    }

    companion object {
        var actionCallbackIntercept: ((GameAction) -> Unit)? = null

        //Singleton
        fun gameActionCallback(action: GameAction) {
            if (actionCallbackIntercept != null) {
                actionCallbackIntercept!!(action)
                return
            }
            GameEngine.acquireCallback(action)
        }

        fun createActionButton(
            index: Int,
            actionName: String,
            checkValidity: Boolean,
            dangerous: Boolean,
            gameState: GameState,
            setActionSheet: (ActionSheetUI) -> Unit,
            actionCallback: (GameAction) -> Unit
        ): Button {
            return scene2d.button("document") {
                val tooltip = ActionTooltipUI(actionName, dangerous)
                addListener(tooltip)
                stack {
                    it.size(100f)
                    image("Help") {
                        color = Color.BLACK
                        if (dangerous) {
                            color = Color.RED
                        }
                        try {
                            this.setDrawable(
                                defaultSkin,
                                ReadOnly.actionJson[actionName]!!.jsonObject["image"]!!.jsonPrimitive.content
                            )
                        } catch (e: Exception) {
                            this.setDrawable(defaultSkin, "Help")
                        }


                        when (actionName) {


                            "Examine" -> {
                                this@button.style = defaultSkin.get("document", Button.ButtonStyle::class.java)
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val examineUI =
                                            ExamineUI(gameState, actionCallback)
                                        setActionSheet(examineUI)
                                    }
                                }
                                )
                            }

                            "Wait" -> {
                                val action = Wait(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        if (gameState.player.currentMeeting != null) {
                                            actionCallback(
                                                Wait(
                                                    gameState.playerName,
                                                    gameState.player.place.name
                                                )
                                            )
                                        } else {
                                            val waitUI =
                                                WaitUI(gameState, actionCallback)
                                            setActionSheet(waitUI)
                                            waitUI.refresh(WaitUIMode.WAIT)
                                        }
                                    }
                                })
                            }

                            "Eat" -> {
                                val action = Eat(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val sound =
                                            Gdx.audio.newSound(Gdx.files.internal(ReadOnly.actionJson["Eat"]!!.jsonObject["sound"]!!.jsonPrimitive.content))
                                        sound.play()
                                        actionCallback(action)
                                    }
                                })
                            }

                            "BuyDrink" -> {
                                val action = BuyDrink(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "Sleep" -> {
                                val action = Sleep(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val waitUI =
                                            WaitUI(gameState, actionCallback)
                                        setActionSheet(waitUI)
                                        waitUI.refresh(WaitUIMode.SLEEP)
                                    }
                                })
                            }

                            "Repair" -> {

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val repUI = RepairUI(gameState, actionCallback)
                                        setActionSheet(repUI)
                                        repUI.refresh(gameState)
                                    }
                                })
                            }

                            "UnofficialResourceTransfer" -> {
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val resUI = ResourceTransferUI(
                                            gameState,
                                            actionCallback
                                        )
                                        setActionSheet(resUI)
                                        resUI.refresh(
                                            "unofficial",
                                            gameState.player.place.resources.toHashMap()
                                        )
                                    }
                                })
                            }

                            "OfficialResourceTransfer" -> {
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val resUI = ResourceTransferUI(
                                            gameState,
                                            actionCallback
                                        )
                                        setActionSheet(resUI)
                                        resUI.refresh(
                                            "official",
                                            gameState.player.place.resources.toHashMap()
                                        )
                                    }
                                })

                            }


                            "AddInfo" -> {
                                if (gameState.informations.none { (key, info) ->
                                        gameState.playerName in info.knownTo
                                    }) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(ReadOnly.prop("addInfo-noPreparedInfo"))
                                } else
                                    if ((gameState.informations.filter {
                                            it.value.knownTo.contains(gameState.playerName)
                                        }.keys - gameState.player.currentMeeting!!.agendas.flatMap { it.informationKeys }).isEmpty()) {
                                        this@button.isDisabled = true
                                        tooltip.displayInvalidReason(ReadOnly.prop("addInfo-noAdditionalInfo"))
                                    } else if ((gameState.player.currentMeeting!!.agendas.isEmpty())) {
                                        this@button.isDisabled = true
                                        tooltip.displayInvalidReason(ReadOnly.prop("addInfo-noAgendas"))
                                    }
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val addInfoUI =
                                            AddInfoUI(gameState, actionCallback)
                                        setActionSheet(addInfoUI)
                                        addInfoUI.refresh()
                                    }
                                })
                            }

                            "EndSpeech" -> {
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val endSpeechUI =
                                            EndSpeechUI(gameState, actionCallback)
                                        setActionSheet(endSpeechUI)
                                    }
                                })
                            }


                            "InvestigateAccidentScene" -> {
                                val action = InvestigateAccidentScene(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "ClearAccidentScene" -> {
                                val action = ClearAccidentScene(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "Intercept" -> {
                                val action = Intercept(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "Resign" -> {
                                val action = Resign(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "Salary" -> {
                                val action = Salary(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "PrepareInfo" -> {
                                if (gameState.informations.filter {
                                        it.value.knownTo.contains(
                                            gameState.playerName
                                        )
                                    }
                                        .isEmpty()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(ReadOnly.prop("prepareInfo-noKnownInfo"))
                                }
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val prepareInfoUI =
                                            PrepareInfoUI(gameState, actionCallback)
                                        setActionSheet(prepareInfoUI)
                                    }
                                })
                            }


                            "JoinMeeting" -> {
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(
                                            JoinMeeting(
                                                gameState.playerName,
                                                gameState.player.place.name
                                            )
                                        )
                                    }
                                })
                            }

                            "StartMeeting" -> {
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(
                                            StartMeeting(
                                                gameState.playerName,
                                                gameState.player.place.name
                                            )
                                        )
                                    }
                                })
                            }


                            "NewAgenda" -> {
                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        val newAgendaUI =
                                            NewAgendaUI(gameState, actionCallback)
                                        setActionSheet(newAgendaUI)
                                        newAgendaUI.refresh(gameState)
                                        //TODO: Logger.warning("New Agenda Action should never be called from AcailableActionsUI, it is called from MeetingUI.")
                                    }
                                })
                            }

                            "LeaveMeeting" -> {
                                val action = LeaveMeeting(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "EndMeeting" -> {
                                val action = EndMeeting(
                                    gameState.playerName,
                                    gameState.player.place.name
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "SetWorkers" -> {
                                val action = SetWorkers(
                                    gameState.playerName,
                                    gameState.player.place.name,
                                    0,
                                    gameState.player.place.apparatuses.first().ID
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }

                            "SetWorkHours" -> {
                                val action = SetWorkHours(
                                    gameState.playerName,
                                    gameState.player.place.name,
                                    0,
                                    24
                                )
                                action.injectParent(gameState); if (checkValidity && !action.isValid()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(action.invalidReason)
                                }

                                this@button.addListener(object : ChangeListener() {
                                    override fun changed(event: ChangeEvent, actor: Actor) {
                                        if (!this@button.isChecked) return
                                        actionCallback(action)
                                    }
                                })
                            }
                            //TODO: also make changes to NewAgendaUI.kt, ActionSelectUI.kt
                            else -> {
                                this.setDrawable(defaultSkin, "Help")

                            }
                        }

                    }
                    container {
                        align(Align.bottomLeft)
                        size(30f)
                        stack {
                            image("white-pixel") {
                                color = Color.BLACK
                            }
                            label(index.toString(), "docTitle") {
                                setFontScale(0.4f)
                                color = Color.WHITE
                                setAlignment(Align.center)
                            }
                        }
                    }
                }

            }
        }
    }


}