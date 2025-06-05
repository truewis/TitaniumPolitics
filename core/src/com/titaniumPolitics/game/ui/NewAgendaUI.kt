package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import com.titaniumPolitics.game.ui.widget.ActionSelectUI
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.buttonGroup


class NewAgendaUI(gameState: GameState, override var actionCallback: (GameAction) -> Unit) : WindowUI("NewAgendaTitle"),
    ActionUI {
    private var subject = gameState.playerName
    val sbjObject = gameState.characters[subject]!!

    lateinit var agenda: MeetingAgenda
    private var availableAgendas = arrayOf<AgendaType>()
    val agendaDetailStack: Stack
    private val actionSelUI = ActionSelectUI(gameState, this::setRequestAction)
    fun setRequestAction(action: GameAction) {
        agenda.attachedRequest = Request(action, hashSetOf(action.sbjCharacter))
    }

    private lateinit var agendaSelectBox: Table
    private val praiseTable = scene2d.table {
        label(ReadOnly.prop("praise")) {
            setFontScale(3f)
        }
        row()
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.PRAISE, this@NewAgendaUI.subject, hashMapOf("character" to selected))
                }
            })
        }.inCell.size(300f, 100f)
    }
    private val denounceTable = scene2d.table {
        label(ReadOnly.prop("denounce")) {
            setFontScale(3f)
        }
        row()
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.DENOUNCE, this@NewAgendaUI.subject, hashMapOf("character" to selected))
                }
            })
        }.inCell.size(300f, 100f)
    }
    private val praisePartyTable = scene2d.table {
        label(ReadOnly.prop("praiseParty")) {
            setFontScale(3f)
        }
        row()
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.parties.keys.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.PRAISE_PARTY, this@NewAgendaUI.subject, hashMapOf("party" to selected))
                }
            })
        }.inCell.size(300f, 100f)
    }
    private val denouncePartyTable = scene2d.table {
        label(ReadOnly.prop("denounceParty")) {
            setFontScale(3f)
        }
        row()
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.parties.keys.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(
                            AgendaType.DENOUNCE_PARTY,
                            this@NewAgendaUI.subject,
                            hashMapOf("party" to selected)
                        )
                }
            })
        }.inCell.size(300f, 100f)
    }

    private val requestTable = scene2d.table {
        label(ReadOnly.prop("request")) {
            setFontScale(3f)
        }
        row()
        label("Transfer resources to") { setFontScale(3f) }
        add(PlaceSelectButton(skin, { this@NewAgendaUI.actionSelUI.changeTgtPlace(it) })).growX()
        row()
        label("Request to:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {//TODO: Replace with Character Selection UI
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.actionSelUI.changeSubject(selected)
                }
            })
        }.inCell.size(300f, 100f)
        row()
        //Select Action
        add(this@NewAgendaUI.actionSelUI).size(1700f, 600f)
    }
    val st = scene2d.stack {
        table {
            this@NewAgendaUI.agendaSelectBox = buttonGroup(0, 1).also {
                it.inCell.size(600f, 150f)
            }

            row()
            //Fill in agenda details.
            this@NewAgendaUI.agendaDetailStack = stack {
                it.grow()
                add(this@NewAgendaUI.praiseTable)
                add(this@NewAgendaUI.denounceTable)
                add(this@NewAgendaUI.praisePartyTable)
                add(this@NewAgendaUI.denouncePartyTable)
                add(this@NewAgendaUI.requestTable)
                //TODO: also make changes to NewAgenda.kt.
            }
            row()
            button {
                it.size(300f, 100f).fill()
                label("Submit") {
                    setFontScale(3f)
                    setAlignment(Align.center)

                }
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        this@NewAgendaUI.actionCallback(
                            NewAgenda(
                                this@NewAgendaUI.subject,
                                this@NewAgendaUI.sbjObject.place.name
                            ).apply { agenda = this@NewAgendaUI.agenda })
                        this@NewAgendaUI.isVisible = false
                    }
                })
            }
//            button {
//                it.fill().size(300f, 100f)
//                label("Cancel") {
//                    setFontScale(3f)
//                    setAlignment(Align.center)
//
//                }
//                addListener(object : ClickListener()
//                {
//                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
//                    {
//                        this@NewAgendaUI.isVisible = false
//                    }
//                })
//            }
        }
    }

    init {
        isVisible = false

        content.add(st).grow()
        hideAllAgendaDetailsTable()


    }

    fun hideAllAgendaDetailsTable() {
        praiseTable.isVisible = false
        denounceTable.isVisible = false
        praisePartyTable.isVisible = false
        denouncePartyTable.isVisible = false
        requestTable.isVisible = false
    }


    fun refresh(gameState: GameState) {
        refreshAvailableAgendaList(gameState)
        actionSelUI.refreshList(listOf("UnofficialResourceTransfer", "OfficialResourceTransfer"))
        availableAgendas.forEach { tobj ->
            val t = scene2d.button {
                //TODO:Agenda Tooltip addListener(ActionTooltipUI(tobj))
                container {
                    it.size(150f)
                    it.fill(0.66f, 0.66f)
                    it.align(Align.center)
                    image("Help") {


                        when (tobj) {
                            //TODO: also make changes to NewAgendaUI.kt.
                            AgendaType.PROOF_OF_WORK -> {
                                this.setDrawable(defaultSkin, "icon_app_147")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    }
                                })
                            }

                            AgendaType.NOMINATE -> {
                                this.setDrawable(defaultSkin, "icon_app_8")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    }
                                })
                            }

                            AgendaType.REQUEST -> {
                                this.setDrawable(defaultSkin, "icon_gesture_58")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.requestTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.PRAISE -> {
                                this.setDrawable(defaultSkin, "icon_gesture_1")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.praiseTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.DENOUNCE -> {
                                this.setDrawable(defaultSkin, "icon_gesture_2")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.denounceTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.PRAISE_PARTY -> {
                                this.setDrawable(defaultSkin, "icon_gesture_1")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.praisePartyTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.DENOUNCE_PARTY -> {
                                this.setDrawable(defaultSkin, "icon_gesture_2")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.denouncePartyTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.BUDGET_PROPOSAL -> {
                                this.setDrawable(defaultSkin, "icon_app_104")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.denouncePartyTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.BUDGET_RESOLUTION -> {
                                this.setDrawable(defaultSkin, "icon_app_105")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.denouncePartyTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.APPOINT_MEETING -> {
                                this.setDrawable(defaultSkin, "icon_app_18")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                        this@NewAgendaUI.denouncePartyTable.isVisible = true
                                    }
                                })
                            }

                            AgendaType.FIRE_MANAGER -> {
                                this.setDrawable(defaultSkin, "icon_app_7")
                                this@button.addListener(object : ClickListener() {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    }
                                })
                            }

                            else -> {
                                this.setDrawable(defaultSkin, "Help")

                            }
                        }

                    }
                }
            }
            agendaSelectBox.add(t).size(150f).fill()
        }
    }

    fun refreshAvailableAgendaList(gameState: GameState) {
        availableAgendas =
            arrayOf(
                AgendaType.PROOF_OF_WORK,
                AgendaType.PRAISE,
                AgendaType.DENOUNCE,
                AgendaType.PRAISE_PARTY,
                AgendaType.DENOUNCE_PARTY,
                AgendaType.REQUEST,
                AgendaType.APPOINT_MEETING
            )
        if (this@NewAgendaUI.sbjObject.currentMeeting == null)
            throw Exception("Player is not in a meeting.")
        val mt = this@NewAgendaUI.sbjObject.currentMeeting!!
        if (mt.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION)
            availableAgendas += AgendaType.NOMINATE
        if (mt.involvedParty == "cabinet" && !gameState.isBudgetProposed)
            availableAgendas += AgendaType.BUDGET_PROPOSAL
        if (mt.involvedParty == "triumvirate" && !gameState.isBudgetResolved)
            availableAgendas += AgendaType.BUDGET_RESOLUTION
        //If the player is a division leader, they can fire managers.
        if (mt.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE && gameState.parties[mt.involvedParty]!!.leader == subject)
            availableAgendas += AgendaType.FIRE_MANAGER
        //TODO: Also update NewAgenda.kt
    }

    override fun changeSubject(charName: String) {
        subject = charName
    }

    companion object {
        //Singleton
        lateinit var primary: NewAgendaUI
    }


}