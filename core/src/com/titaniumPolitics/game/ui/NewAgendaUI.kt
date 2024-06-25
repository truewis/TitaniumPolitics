package com.titaniumPolitics.game.ui


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class NewAgendaUI(gameState: GameState, override var actionCallback: (GameAction) -> Unit) : Table(defaultSkin), KTable,
    ActionUI
{
    private val dataTable = Table()
    private val targetTable = Table()

    var toWhere = ""
    var toWho = ""
    lateinit var agenda: MeetingAgenda
    val agendaDetailStack: Stack
    private val actionButtonList = HorizontalGroup()
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
                    this@NewAgendaUI.agenda = MeetingAgenda("praise", hashMapOf("character" to selected))
                }
            })
        }
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
                    this@NewAgendaUI.agenda = MeetingAgenda("denounce", hashMapOf("character" to selected))
                }
            })
        }
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
                    this@NewAgendaUI.agenda = MeetingAgenda("praiseParty", hashMapOf("party" to selected))
                }
            })
        }
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
                    this@NewAgendaUI.agenda = MeetingAgenda("denounceParty", hashMapOf("party" to selected))
                }
            })
        }
    }

    private val requestTable = scene2d.table {
        button {
            it.colspan(2).growX()
            val placeLabel = label("Request Place:", "trnsprtConsole") { setFontScale(3f) }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    PlaceSelectionUI.instance.isVisible = true
                    PlaceSelectionUI.instance.refresh()
                    PlaceSelectionUI.instance.selectedPlaceCallback = {
                        placeLabel.setText("Request Place: $it")
                        this@NewAgendaUI.toWhere = it
                    }
                }
            })
        }
        row()
        label("Request to:", "trnsprtConsole") { setFontScale(3f) }
        //Select character to perform the request.
        selectBox<String> {
            items = Array(gameState.characters.keys.toTypedArray())
            addListener(object : ChangeListener()
            {
                override fun changed(event: ChangeEvent?, actor: Actor?)
                {
                    this@NewAgendaUI.toWho = selected
                }
            })
        }
        row()
        //Select Action
        add(this@NewAgendaUI.actionButtonList)
    }

    init
    {
        isVisible = false
        instance = this
        stack {
            it.grow()
            image("panel") {
            }
            table {
                label("New Agenda", "trnsprtConsole") {
                    setFontScale(3f)
                }
                this@NewAgendaUI.agendaSelectBox = selectBox {
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

                }

                row()
                //Fill in agenda details.
                this@NewAgendaUI.agendaDetailStack = stack {
                    it.grow()
                    image("panel") {
                    }
                    add(this@NewAgendaUI.praiseTable)
                    add(this@NewAgendaUI.denounceTable)
                    add(this@NewAgendaUI.praisePartyTable)
                    add(this@NewAgendaUI.denouncePartyTable)
                    add(this@NewAgendaUI.requestTable)
                    //TODO: also make changes to NewAgenda.kt.
                }
                row()
                button {
                    it.fill()
                    label("Submit") {
                        setAlignment(Align.center)

                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@NewAgendaUI.actionCallback(
                                NewAgenda(
                                    gameState.playerName,
                                    gameState.player.place.name
                                ).apply { agenda = this@NewAgendaUI.agenda })
                            this@NewAgendaUI.isVisible = false
                        }
                    })
                }
                button {
                    it.fill()
                    label("Cancel") {
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


    }

    fun hideAllAgendaDetailsTable()
    {
        praiseTable.isVisible = false
        denounceTable.isVisible = false
        praisePartyTable.isVisible = false
        denouncePartyTable.isVisible = false
        requestTable.isVisible = false
    }


    fun refresh()
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
        if (gameState.player.currentMeeting == null)
            throw Exception("Player is not in a meeting.")
        val mt = gameState.player.currentMeeting!!
        if (mt.type == "divisionLeaderElection")
            agendas += "nomination"
        if (mt.involvedParty == "cabinet" && !gameState.isBudgetProposed)
            agendas += "budgetProposal"
        if (mt.involvedParty == "triumvirate" && !gameState.isBudgetResolved)
            agendas += "budgetResolution"
        //TODO: Also update NewAgenda.kt
        agendaSelectBox.items = Array(agendas)
    }

    //TODO: also make changes to NewAgendaUI.kt.
    fun refreshActionList(gameState: GameState)
    {

        GameEngine.availableActions(
            gameState,
            gameState.player.place.name,
            gameState.playerName
        ).forEach { tobj ->
            //We do not create buttons for these actions, as they are accessible through the main UI.
            if (listOf("Move", "Talk").contains(tobj))
            {
                return@forEach
            }
            val t = scene2d.button {
                textTooltip(ReadOnly.prop(tobj) + "\n" + ReadOnly.prop("$tobj-description"), "default") {
                    this.setFontScale(2f)
                    it.manager.initialTime = 0.5f
                }
                image("question-mark-circle-outline-icon") {
                    it.size(100f)

                    when (tobj)
                    {


                        "Trade" ->
                        {
                            this.setDrawable(defaultSkin, "hand-shake-icon")
                        }

                        "Examine" ->
                        {
                            this.setDrawable(defaultSkin, "magnifying-glass-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {

                                }
                            }
                            )
                        }

                        "Wait" ->
                        {
                            this.setDrawable(defaultSkin, "sand-clock-half-line-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    val sound =
                                        Gdx.audio.newSound(Gdx.files.internal(ReadOnly.actionJson["Wait"]!!.jsonObject["sound"]!!.jsonPrimitive.content))
                                    sound.play()

                                    GameEngine.acquireCallback(
                                        Wait(
                                            gameState.playerName,
                                            gameState.player.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "Eat" ->
                        {
                            this.setDrawable(defaultSkin, "food-dinner-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    val sound =
                                        Gdx.audio.newSound(Gdx.files.internal(ReadOnly.actionJson["Eat"]!!.jsonObject["sound"]!!.jsonPrimitive.content))
                                    sound.play()
                                    GameEngine.acquireCallback(
                                        Eat(
                                            gameState.playerName,
                                            gameState.player.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "Sleep" ->
                        {
                            this.setDrawable(defaultSkin, "closed-eye-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    GameEngine.acquireCallback(
                                        Sleep(
                                            gameState.playerName,
                                            gameState.player.place.name
                                        )
                                    )
                                }
                            })
                        }

                        "Repair" ->
                        {
                            this.setDrawable(defaultSkin, "hammer-line-icon")
                        }

                        "UnofficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "boxes-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    ResourceTransferUI.instance.isVisible = true
                                    ResourceTransferUI.instance.refresh(
                                        "unofficial",
                                        GameEngine.acquireCallback,
                                        gameState.player.place.resources
                                    )
                                }
                            })
                        }

                        "OfficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "boxes-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    ResourceTransferUI.instance.isVisible = true
                                    ResourceTransferUI.instance.refresh(
                                        "official",
                                        GameEngine.acquireCallback,
                                        gameState.player.place.resources
                                    )
                                }
                            })
                        }

                        "JoinMeeting" ->
                        {
                            this.setDrawable(defaultSkin, "speaking-bubbles-line-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    GameEngine.acquireCallback(
                                        JoinMeeting(
                                            gameState.playerName,
                                            gameState.player.place.name
                                        ).also {
                                            it.meetingName = gameState.ongoingMeetings.filter {
                                                it.value.scheduledCharacters.contains(gameState.playerName) && it.value.place == gameState.player.place.name
                                            }.keys.first()
                                        }
                                    )
                                }
                            })
                        }

                        "JoinConference" ->
                        {
                            this.setDrawable(defaultSkin, "speaking-bubbles-line-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    GameEngine.acquireCallback(
                                        JoinConference(
                                            gameState.playerName,
                                            gameState.player.place.name
                                        ).also {
                                            it.meetingName =
                                                gameState.ongoingConferences.filter {
                                                    it.value.scheduledCharacters.contains(gameState.playerName) && it.value.place == gameState.player.place.name
                                                }.keys.first()
                                        }
                                    )
                                }
                            })
                        }

                        "NewAgenda" ->
                        {
                            this.setDrawable(defaultSkin, "plus-circle-line-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    NewAgendaUI.instance.isVisible = true
                                    NewAgendaUI.instance.refresh()
                                }
                            })
                        }

                        "LeaveMeeting" ->
                        {
                            this.setDrawable(defaultSkin, "close-square-line-icon")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    GameEngine.acquireCallback(
                                        LeaveMeeting(
                                            gameState.playerName,
                                            gameState.player.place.name
                                        )

                                    )
                                }
                            })
                        }
                        //TODO: also make changes to NewAgendaUI.kt.
                        else ->
                        {
                            this.setDrawable(defaultSkin, "question-mark-circle-outline-icon")

                        }
                    }

                }
            }
            actionButtonList.addActor(t)
        }
        isVisible = !actionButtonList.children.isEmpty

    }

    companion object
    {
        //Singleton
        lateinit var instance: NewAgendaUI
    }


}