package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.ui.widget.ActionSelectButton
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton
import com.titaniumPolitics.game.ui.widget.TitleLabel
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.collections.get


class NewAgendaUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("NewAgendaTitle", gameState, actionCallback) {
    val sbjChar = gameState.characters[subject]!!
    var agenda: MeetingAgenda? = null
        set(value) {
            field = value
            if (value != null)
                submitButton.refresh(
                    NewAgenda(this.subject, this.tgtPlace, gameState).apply { agenda = value })


        }
    private var availableAgendas = arrayOf<AgendaType>()
    val agendaDetailStack: Stack
    private val actionSelButton = ActionSelectButton(this::setRequestAction)
    fun setRequestAction(action: GameAction) {
        agenda = MeetingAgenda(AgendaType.REQUEST, subject).apply {
            attachedRequest = Request(action, hashSetOf(action.sbjCharacter), hashSetOf(subject))
        }
    }

    private var agendaSelectBox: Table
    private val proofOfWorkTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-proofOfWork"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-proofOfWork-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        selectBox<String> {
            items =
                Array(this@NewAgendaUI.gameState.requests.filter { !it.value.completed && this@NewAgendaUI.subject in it.value.issuedBy }.keys.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.PROOF_OF_WORK, this@NewAgendaUI.subject).also {
                            it.attachedRequest = this@NewAgendaUI.gameState.requests[selected]
                        }
                }
            })
        }.inCell.size(300f, 70f)
    }
    private val praisableCharacters = gameState.characters.filter {
        it.value.type == Character.Type.DIRECTOR || gameState.parties[sbjChar.currentMeeting?.involvedParty]?.members?.contains(
            it.value.name
        ) == true
    }.map { it.value.name }.toSet()
    private val praiseTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-praise"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-praise-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        add(CharacterSelectButton(this@NewAgendaUI.praisableCharacters) { char ->
            this@NewAgendaUI.agenda =
                MeetingAgenda(AgendaType.PRAISE, this@NewAgendaUI.subject, hashMapOf("character" to char))
        }).size(180f)
    }
    private val denounceTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-denounce"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-denounce-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        add(CharacterSelectButton(this@NewAgendaUI.praisableCharacters) { char ->
            this@NewAgendaUI.agenda =
                MeetingAgenda(AgendaType.DENOUNCE, this@NewAgendaUI.subject, hashMapOf("character" to char))
        }).size(180f)
    }
    private val praisableParty = gameState.parties.filter {
        it.value.type == Party.Type.DIVISION ||
                (it.value.type == Party.Type.WORKPLACE && it.value.workplace.responsibleDivision?.let { it == sbjChar.division?.name } ?: false) ||
                (it.value.type == Party.Type.QUALIFICATION)
    }.keys.toSet()
    private val praisePartyTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-praiseParty"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-praiseParty-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        selectBox<String> {
            items = Array(this@NewAgendaUI.praisableParty.toTypedArray())
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    this@NewAgendaUI.agenda =
                        MeetingAgenda(AgendaType.PRAISE_PARTY, this@NewAgendaUI.subject, hashMapOf("party" to selected))
                }
            })
        }.inCell.size(300f, 70f)
    }
    private val denouncePartyTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-denounceParty"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-denounceParty-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        selectBox<String> {
            items = Array(this@NewAgendaUI.praisableParty.toTypedArray())
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
        }.inCell.size(300f, 70f)
    }

    private val requestTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-request"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-request-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        table {
            val csButton =
                CharacterSelectButton(this@NewAgendaUI.sbjChar.currentMeeting!!.currentCharacters.filter { it != this@NewAgendaUI.subject }
                    .toSet()//TODO: this only works because we don't have to refresh the character list, because everything happens in the same turn.
                ) { char ->
                    this@NewAgendaUI.actionSelButton.changeSubject(char)
                }
            add(csButton).size(180f)
            add(PlaceSelectButton({
                this@NewAgendaUI.actionSelButton.changeTgtPlace(it)
            })).size(300f, 150f)
        }
        row()
        //Select Action
        add(this@NewAgendaUI.actionSelButton).size(300f, 150f)
    }
    private val fireTable = scene2d.table {
        add(TitleLabel(ReadOnly.prop("NewAgendaUI-fireManager"))).growX()
        row()
        label(ReadOnly.prop("NewAgendaUI-fireManager-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        add(CharacterSelectButton(this@NewAgendaUI.sbjChar.currentMeeting!!.involvedParty?.let { partyName ->
            (this@NewAgendaUI.gameState.parties[partyName]!!.members - this@NewAgendaUI.sbjChar.name).toSet()
        }) { char ->
            this@NewAgendaUI.agenda =
                MeetingAgenda(AgendaType.FIRE_MANAGER, this@NewAgendaUI.subject, hashMapOf("character" to char))
        }).size(180f)
    }
    private val budgetProposalTable = scene2d.table {
        label(ReadOnly.prop("NewAgendaUI-budgetProposal"), "docTitle") {
            color = Color.BLACK
        }
        row()
        label(ReadOnly.prop("NewAgendaUI-budgetProposal-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        row()
        add(CharacterSelectButton(this@NewAgendaUI.sbjChar.currentMeeting!!.involvedParty?.let { partyName ->
            (this@NewAgendaUI.gameState.parties[partyName]!!.members - this@NewAgendaUI.sbjChar.name).toSet()
        }) { char ->
            this@NewAgendaUI.agenda =
                MeetingAgenda(AgendaType.FIRE_MANAGER, this@NewAgendaUI.subject, hashMapOf("character" to char))
        }).size(180f)
    }
    private val budgetResolutionTable = scene2d.table {
        label(ReadOnly.prop("NewAgendaUI-budgetProposal"), "docTitle") {
            color = Color.BLACK
        }
        row()
        label(ReadOnly.prop("NewAgendaUI-budgetResolution-desc"), "docTitle") {
            color = Color.BLACK; setFontScale(0.3f); wrap = true; it.fill()
        }
        //Select character to perform the request.
        add(CharacterSelectButton(this@NewAgendaUI.sbjChar.currentMeeting!!.involvedParty?.let { partyName ->
            (this@NewAgendaUI.gameState.parties[partyName]!!.members - this@NewAgendaUI.sbjChar.name).toSet()
        }) { char ->
            this@NewAgendaUI.agenda =
                MeetingAgenda(AgendaType.FIRE_MANAGER, this@NewAgendaUI.subject, hashMapOf("character" to char))
        }).size(180f)
    }
    val st = scene2d.stack {
        table {
            this@NewAgendaUI.agendaSelectBox = buttonGroup(1, 1).also {
                it.inCell.size(500f, 100f)
            }

            row()
            //Fill in agenda details.
            this@NewAgendaUI.agendaDetailStack = stack {
                it.grow()
                add(this@NewAgendaUI.proofOfWorkTable)
                add(this@NewAgendaUI.praiseTable)
                add(this@NewAgendaUI.denounceTable)
                add(this@NewAgendaUI.praisePartyTable)
                add(this@NewAgendaUI.denouncePartyTable)
                add(this@NewAgendaUI.requestTable)
                add(this@NewAgendaUI.fireTable)
                add(this@NewAgendaUI.budgetProposalTable)
                add(this@NewAgendaUI.budgetResolutionTable)
                //TODO: also make changes to NewAgenda.kt.
            }
            row()
            add(this@NewAgendaUI.submitButton).size(200f, 75f).fill()
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

        content.add(st).grow()
        hideAllAgendaDetailsTable()


    }

    fun hideAllAgendaDetailsTable() {
        proofOfWorkTable.isVisible = false
        praiseTable.isVisible = false
        denounceTable.isVisible = false
        praisePartyTable.isVisible = false
        denouncePartyTable.isVisible = false
        requestTable.isVisible = false
        fireTable.isVisible = false
        budgetProposalTable.isVisible = false
        budgetResolutionTable.isVisible = false
        agenda = null
    }


    fun refresh(gameState: GameState) {
        agendaSelectBox.clear()
        refreshAvailableAgendaList(gameState)
        actionSelButton.refreshList(
            listOf(
                "Examine",
                "UnofficialResourceTransfer",
                "OfficialResourceTransfer",
                "Repair",
                "Salary",
                "SetWorkers",
                "SetWorkHours",
                "InvestigateAccidentScene",
                "ClearAccidentScene"
            )
        )
        availableAgendas.forEach { tobj ->
            val t = scene2d.button("check") {
                //TODO:Agenda Tooltip addListener(ActionTooltipUI(tobj))
                container {
                    it.size(80f)
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
                                        this@NewAgendaUI.proofOfWorkTable.isVisible = true
                                        this@NewAgendaUI.agenda = MeetingAgenda(
                                            AgendaType.PROOF_OF_WORK, this@NewAgendaUI.sbjChar.name
                                        )
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
                                this@button.isChecked = true
                                this@NewAgendaUI.hideAllAgendaDetailsTable()
                                this@NewAgendaUI.praiseTable.isVisible = true
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
                                        this@NewAgendaUI.fireTable.isVisible = true
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
            agendaSelectBox.add(t).size(100f).fill()
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
                AgendaType.APPOINT_MEETING,
            )
        if (this@NewAgendaUI.sbjChar.currentMeeting == null)
            throw Exception("Player is not in a meeting.")
        val mt = this@NewAgendaUI.sbjChar.currentMeeting!!

        val party = gameState.parties[mt.involvedParty]
        if (mt.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION)
            availableAgendas += AgendaType.NOMINATE
        if (mt.involvedParty in listOf("cabinet", "division") && !party!!.isBudgetProposed)
            availableAgendas += AgendaType.BUDGET_PROPOSAL
        if (mt.involvedParty in listOf("triumvirate", "division") && !party!!.isBudgetResolved)
            availableAgendas += AgendaType.BUDGET_RESOLUTION
        //If the player is a division leader, they can fire managers.
        if (mt.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE && gameState.parties[mt.involvedParty]!!.leader == subject)
            availableAgendas += AgendaType.FIRE_MANAGER
        //TODO: Also update NewAgenda.kt
    }


}