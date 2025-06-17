package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableActionsUI(var gameState: GameState) : Table(defaultSkin), KTable {
    private val docList = HorizontalGroup()
    val options: ExamineUI = ExamineUI(this@AvailableActionsUI.gameState)

    val dateLabel: Label
    val timeLabel: Label
    val placeLabel: Label

    init {

        options.isVisible = false
        gameState.updateUI += { _ -> refreshList(); }

        val docScr = ScrollPane(docList)
        docList.align(Align.center)
        docList.grow()
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
//        CapsuleStage.instance.onMouseDown.add { x, y ->
//            //If x and y are not within the bounds of this UI, hide the option ui.
//            val localpos = options.screenToLocalCoordinates(Vector2(x, y))
//            if (options.hit(localpos.x, localpos.y, true) == null)
//            {
//                options.isVisible = false
//            }
//        }

                    label("Action Performed", "docTitle") {
                        it.center()
                        setFontScale(0.25f)
                        color = Color.BLACK
                        setAlignment(Align.center)
                    }
                    row()
                    add(docScr).size(900f, 150f)
                    row()
                    add(this@AvailableActionsUI.options)
                }
            }
        }


    }

    //TODO: also make changes to NewAgendaUI.kt.
    fun refreshList() {
        docList.clear()
        dateLabel.setText(gameState.formatDate())
        timeLabel.setText(gameState.formatClock())
        placeLabel.setText(ReadOnly.prop(gameState.player.place.name))
        GameEngine.availableActions(
            gameState,
            gameState.player.place.name,
            gameState.playerName
        ).forEach { tobj ->
            //We do not create buttons for these actions, as they are accessible through the main UI.
            if (listOf("Move", "Talk").contains(tobj)) {
                return@forEach
            }
            val t = scene2d.button("document") {
                val tooltip = ActionTooltipUI(tobj)
                addListener(tooltip)
                image("Help") {
                    it.size(80f)
                    color = Color.BLACK

                    when (tobj) {


                        "Examine" -> {
                            this@button.style = defaultSkin.get("document", Button.ButtonStyle::class.java)
                            this.setDrawable(defaultSkin, "SearchGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    this@AvailableActionsUI.options.isVisible =
                                        !this@AvailableActionsUI.options.isVisible
                                }
                            }
                            )
                        }

                        "Wait" -> {
                            this.setDrawable(defaultSkin, "DotsGrunge")
                            val action = Wait(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    if (this@AvailableActionsUI.gameState.player.currentMeeting != null) {
                                        GameEngine.acquireCallback(
                                            Wait(
                                                this@AvailableActionsUI.gameState.playerName,
                                                this@AvailableActionsUI.gameState.player.place.name
                                            )
                                        )
                                    } else {
                                        WaitUI.primary.isVisible = true
                                        WaitUI.primary.actionCallback = GameEngine.acquireCallback
                                        WaitUI.primary.refresh(WaitUIMode.WAIT)
                                    }
                                }
                            })
                        }

                        "Eat" -> {
                            this.setDrawable(defaultSkin, "AppleGrunge")
                            val action = Eat(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    val sound =
                                        Gdx.audio.newSound(Gdx.files.internal(ReadOnly.actionJson["Eat"]!!.jsonObject["sound"]!!.jsonPrimitive.content))
                                    sound.play()
                                    GameEngine.acquireCallback(action)
                                    ProgressBackgroundUI.instance.text = ReadOnly.prop("Eating")
                                    ProgressBackgroundUI.instance.setVisibleWithFade(true)
                                }
                            })
                        }

                        "Sleep" -> {
                            this.setDrawable(defaultSkin, "icon_activity_117")
                            val action = Sleep(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    WaitUI.primary.isVisible = true
                                    WaitUI.primary.actionCallback = GameEngine.acquireCallback
                                    WaitUI.primary.refresh(WaitUIMode.SLEEP)
                                }
                            })
                        }

                        "Repair" -> {
                            this.setDrawable(defaultSkin, "CogGrunge")
                            val action = Repair(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                    ProgressBackgroundUI.instance.text = ReadOnly.prop("Repairing")
                                    ProgressBackgroundUI.instance.setVisibleWithFade(true)
                                }
                            })
                        }

                        "UnofficialResourceTransfer" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    ResourceTransferUI.primary.isVisible = true
                                    ResourceTransferUI.primary.refresh(
                                        "unofficial",
                                        {
                                            GameEngine.acquireCallback(it)
                                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Transferring")
                                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
                                        },
                                        this@AvailableActionsUI.gameState.player.place.resources.toHashMap()
                                    )
                                }
                            })
                        }

                        "OfficialResourceTransfer" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    ResourceTransferUI.primary.isVisible = true
                                    ResourceTransferUI.primary.refresh(
                                        "official",
                                        {
                                            GameEngine.acquireCallback(it)
                                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Transferring")
                                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
                                        },
                                        this@AvailableActionsUI.gameState.player.place.resources.toHashMap()
                                    )
                                }
                            })

                        }


                        "AddInfo" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            if (this@AvailableActionsUI.gameState.player.preparedInfoKeys.isEmpty()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(ReadOnly.prop("addInfo-noPreparedInfo"))
                            } else
                                if ((this@AvailableActionsUI.gameState.player.preparedInfoKeys - this@AvailableActionsUI.gameState.player.currentMeeting!!.agendas.flatMap { it.informationKeys }).isEmpty()) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(ReadOnly.prop("addInfo-noAdditionalInfo"))
                                } else if ((this@AvailableActionsUI.gameState.player.currentMeeting!!.agendas.isEmpty())) {
                                    this@button.isDisabled = true
                                    tooltip.displayInvalidReason(ReadOnly.prop("addInfo-noAgendas"))
                                }
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    AddInfoUI.primary.isVisible = true
                                    AddInfoUI.primary.actionCallback = GameEngine.acquireCallback
                                    AddInfoUI.primary.refresh()
                                }
                            })
                        }

                        "EndSpeech" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    EndSpeechUI.primary.isVisible = true
                                    EndSpeechUI.primary.actionCallback = GameEngine.acquireCallback
                                    EndSpeechUI.primary.refresh()
                                }
                            })
                        }


                        "InvestigateAccidentScene" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            val action = InvestigateAccidentScene(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Investigating")
                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
                        }

                        "ClearAccidentScene" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            val action = ClearAccidentScene(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                            ProgressBackgroundUI.instance.text = ReadOnly.prop("Clearing")
                            ProgressBackgroundUI.instance.setVisibleWithFade(true)
                        }

                        "Intercept" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            val action = Intercept(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                        }

                        "Resign" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            val action = Resign(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                        }

                        "Salary" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            val action = Salary(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                        }

                        "PrepareInfo" -> {
                            this.setDrawable(defaultSkin, "TilesGrunge")
                            if (this@AvailableActionsUI.gameState.informations.filter { it.value.knownTo.contains(this@AvailableActionsUI.gameState.playerName) }
                                    .isEmpty()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(ReadOnly.prop("prepareInfo-noKnownInfo"))
                            }
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    InformationViewUI.instance.isVisible = true
                                    InformationViewUI.instance.refresh("tgtTime", InformationViewMode.SELECT) { keys ->
                                        GameEngine.acquireCallback(
                                            PrepareInfo(
                                                this@AvailableActionsUI.gameState.playerName,
                                                this@AvailableActionsUI.gameState.player.place.name
                                            ).also {
                                                it.newSetOfPrepInfoKeys = ArrayList(keys)
                                            })
                                        ProgressBackgroundUI.instance.text = ReadOnly.prop("Preparing")
                                        ProgressBackgroundUI.instance.setVisibleWithFade(true)
                                    }
                                }
                            })
                        }


                        "JoinMeeting" -> {
                            this.setDrawable(defaultSkin, "ChatGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(
                                        JoinMeeting(
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        ).also {
                                            it.meetingName = this@AvailableActionsUI.gameState.ongoingMeetings.filter {
                                                it.value.scheduledCharacters.contains(this@AvailableActionsUI.gameState.playerName) && it.value.place == this@AvailableActionsUI.gameState.player.place.name
                                            }.keys.first()
                                        }
                                    )
                                }
                            })
                        }

                        "StartMeeting" -> {
                            this.setDrawable(defaultSkin, "ChatGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(
                                        StartMeeting(
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        ).also {
                                            it.meetingName =
                                                this@AvailableActionsUI.gameState.scheduledMeetings.filter {
                                                    it.value.scheduledCharacters.contains(this@AvailableActionsUI.gameState.playerName) && it.value.place == this@AvailableActionsUI.gameState.player.place.name
                                                }.keys.first()
                                        }
                                    )
                                }
                            })
                        }


                        "NewAgenda" -> {
                            this.setDrawable(defaultSkin, "PlusGrunge")
                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    NewAgendaUI.primary.isVisible = true
                                    NewAgendaUI.primary.actionCallback = GameEngine.acquireCallback
                                    NewAgendaUI.primary.refresh(this@AvailableActionsUI.gameState)
                                }
                            })
                        }

                        "LeaveMeeting" -> {
                            this.setDrawable(defaultSkin, "XGrunge")
                            val action = LeaveMeeting(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                        }

                        "EndMeeting" -> {
                            this.setDrawable(defaultSkin, "XGrunge")
                            val action = EndMeeting(
                                this@AvailableActionsUI.gameState.playerName,
                                this@AvailableActionsUI.gameState.player.place.name
                            )
                            action.injectParent(this@AvailableActionsUI.gameState); if (!action.isValid()) {
                                this@button.isDisabled = true
                                tooltip.displayInvalidReason(action.invalidReason)
                            }

                            this@button.addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    GameEngine.acquireCallback(action)
                                }
                            })
                        }
                        //TODO: also make changes to NewAgendaUI.kt, ActionSelectUI.kt
                        else -> {
                            this.setDrawable(defaultSkin, "Help")

                        }
                    }

                }
            }
            docList.addActor(scene2d.container(t) {
                size(150f)
            })
        }
        isVisible = !docList.children.isEmpty

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
    }


}