package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI

import ktx.scene2d.*


class NewAgendaUI(gameState: GameState, override var actionCallback: (GameAction) -> Unit) : WindowUI("NewAgendaTitle"),
    ActionUI
{
    private var subject = gameState.playerName
    val sbjObject = gameState.characters[subject]!!
    private val dataTable = Table()
    private val targetTable = Table()

    lateinit var agenda: MeetingAgenda
    val agendaDetailStack: Stack
    private val actionSelUI = ActionSelectUI(gameState, this::setRequestAction)
    fun setRequestAction(action: GameAction)
    {
        agenda.attachedRequest = Request(action, hashSetOf(action.sbjCharacter))
    }

    private val agendaSelectBox: SelectBox<String>
    private val praiseTable = scene2d.table {
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener()
            {
                override fun changed(event: ChangeEvent?, actor: Actor?)
                {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.PRAISE, this@NewAgendaUI.subject, hashMapOf("character" to selected))
                }
            })
        }.inCell.size(300f, 100f)
    }
    private val denounceTable = scene2d.table {
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener()
            {
                override fun changed(event: ChangeEvent?, actor: Actor?)
                {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.DENOUNCE, this@NewAgendaUI.subject, hashMapOf("character" to selected))
                }
            })
        }.inCell.size(300f, 100f)
    }
    private val praisePartyTable = scene2d.table {
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.parties.keys.toTypedArray())
            addListener(object : ChangeListener()
            {
                override fun changed(event: ChangeEvent?, actor: Actor?)
                {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.PRAISE_PARTY, this@NewAgendaUI.subject, hashMapOf("party" to selected))
                }
            })
        }.inCell.size(300f, 100f)
    }
    private val denouncePartyTable = scene2d.table {
        label("Target:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.parties.keys.toTypedArray())
            addListener(object : ChangeListener()
            {
                override fun changed(event: ChangeEvent?, actor: Actor?)
                {
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
        button {
            it.colspan(2).growX().size(300f, 100f)
            val placeLabel = label("Request Place:", "trnsprtConsole") { setFontScale(3f) }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    PlaceSelectionUI.instance.isVisible = true
                    PlaceSelectionUI.instance.refresh()
                    PlaceSelectionUI.instance.selectedPlaceCallback = {
                        placeLabel.setText("Request Place: $it")
                        this@NewAgendaUI.actionSelUI.changeTgtPlace(it)
                    }
                }
            })
        }
        row()
        label("Request to:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {//TODO: Replace with Character Selection UI
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener()
            {
                override fun changed(event: ChangeEvent?, actor: Actor?)
                {
                    this@NewAgendaUI.actionSelUI.changeSubject(selected)
                }
            })
        }.inCell.size(300f, 100f)
        row()
        //Select Action
        add(this@NewAgendaUI.actionSelUI)
    }

    init
    {
        isVisible = false
        val st = stack {
            it.grow()
            table {
                this@NewAgendaUI.agendaSelectBox = selectBox<String> {

                    items = Array(
                        arrayOf(
                            "proofOfWork",
                            "praise",
                            "denounce",
                            "praiseParty",
                            "denounceParty",
                            "request"
                        )
                    )
                    //TODO: Also update NewAgenda.kt
                    addListener(object : ChangeListener()
                    {
                        override fun changed(event: ChangeEvent?, actor: Actor?)
                        {
                            when (selected)
                            {
                                "proofOfWork" ->
                                {
                                    this@NewAgendaUI.hideAllAgendaDetailsTable()
                                }

                                "praise" ->
                                {
                                    this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    this@NewAgendaUI.praiseTable.isVisible = true
                                }

                                "denounce" ->
                                {
                                    this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    this@NewAgendaUI.denounceTable.isVisible = true
                                }

                                "praiseParty" ->
                                {
                                    this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    this@NewAgendaUI.praisePartyTable.isVisible = true
                                }

                                "denounceParty" ->
                                {
                                    this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    this@NewAgendaUI.denouncePartyTable.isVisible = true
                                }

                                "request" ->
                                {
                                    this@NewAgendaUI.hideAllAgendaDetailsTable()
                                    this@NewAgendaUI.requestTable.isVisible = true
                                }
                            }
                        }
                    })

                }.also { it.inCell.size(300f, 100f) }

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
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@NewAgendaUI.actionCallback(
                                NewAgenda(
                                    this@NewAgendaUI.subject,
                                    this@NewAgendaUI.sbjObject.place.name
                                ).apply { agenda = this@NewAgendaUI.agenda })
                            this@NewAgendaUI.isVisible = false
                        }
                    })
                }
                button {
                    it.fill().size(300f, 100f)
                    label("Cancel") {
                        setFontScale(3f)
                        setAlignment(Align.center)

                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@NewAgendaUI.isVisible = false
                        }
                    })
                }
            }
        }
        content.add(st).grow()
        hideAllAgendaDetailsTable()


    }

    fun hideAllAgendaDetailsTable()
    {
        praiseTable.isVisible = false
        denounceTable.isVisible = false
        praisePartyTable.isVisible = false
        denouncePartyTable.isVisible = false
        requestTable.isVisible = false
    }


    fun refresh(gameState: GameState)
    {
        dataTable.clear()
        dataTable.apply {
            add(table {

            })
        }

        targetTable.clear()
        targetTable.apply {
            add(table {

            })
        }
        refreshAvailableAgendaList(gameState)
        actionSelUI.refreshList(listOf("UnofficialResourceTransfer", "OfficialResourceTransfer"))
    }

    fun refreshAvailableAgendaList(gameState: GameState)
    {
        var agendas =
            arrayOf(
                "proofOfWork",
                "praise",
                "denounce",
                "praiseParty",
                "denounceParty",
                "request",
                "appointMeeting"
            )
        if (this@NewAgendaUI.sbjObject.currentMeeting == null)
            throw Exception("Player is not in a meeting.")
        val mt = this@NewAgendaUI.sbjObject.currentMeeting!!
        if (mt.type == "divisionLeaderElection")
            agendas += "nomination"
        if (mt.involvedParty == "cabinet" && !gameState.isBudgetProposed)
            agendas += "budgetProposal"
        if (mt.involvedParty == "triumvirate" && !gameState.isBudgetResolved)
            agendas += "budgetResolution"
        //TODO: Also update NewAgenda.kt
        agendaSelectBox.items = Array(agendas)
    }

    override fun changeSubject(charName: String)
    {
        subject = charName
    }

    companion object
    {
        //Singleton
        lateinit var primary: NewAgendaUI
    }


}