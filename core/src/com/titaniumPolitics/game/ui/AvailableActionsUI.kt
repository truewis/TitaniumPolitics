package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AvailableActionsUI(var gameState: GameState) : Table(defaultSkin), KTable
{
    private val docList = HorizontalGroup()
    val options: ExamineUI

    init
    {


        options = ExamineUI(this@AvailableActionsUI.gameState)
        add(options)
        options.isVisible = false
//        CapsuleStage.instance.onMouseDown.add { x, y ->
//            //If x and y are not within the bounds of this UI, hide the option ui.
//            val localpos = options.screenToLocalCoordinates(Vector2(x, y))
//            if (options.hit(localpos.x, localpos.y, true) == null)
//            {
//                options.isVisible = false
//            }
//        }
        row()
        val docScr = ScrollPane(docList)
        docList.align(Align.center)
        docList.grow()

        add(docScr).size(1200f, 150f)
        gameState.updateUI += { _ -> refreshList(); }
    }

    //TODO: also make changes to NewAgendaUI.kt.
    fun refreshList()
    {
        docList.clear()
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
                addListener(ActionTooltipUI(tobj))
                image("Help") {
                    it.size(100f)

                    when (tobj)
                    {


                        "Examine" ->
                        {
                            this.setDrawable(defaultSkin, "SearchGrunge")
                            this@button.addListener(object : ClickListener()
                            {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    this@AvailableActionsUI.options.isVisible =
                                        !this@AvailableActionsUI.options.isVisible
                                }
                            }
                            )
                        }

                        "Wait" ->
                        {
                            this.setDrawable(defaultSkin, "DotsGrunge")
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "Eat" ->
                        {
                            this.setDrawable(defaultSkin, "AppleGrunge")
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        )
                                    )
                                }
                            }
                            )
                        }

                        "Sleep" ->
                        {
                            this.setDrawable(defaultSkin, "icon_activity_117")
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        )
                                    )
                                }
                            })
                        }

                        "Repair" ->
                        {
                            this.setDrawable(defaultSkin, "CogGrunge")
                        }

                        "UnofficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "TilesGrunge")
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
                                        this@AvailableActionsUI.gameState.player.place.resources
                                    )
                                }
                            })
                        }

                        "OfficialResourceTransfer" ->
                        {
                            this.setDrawable(defaultSkin, "TilesGrunge")
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
                                        this@AvailableActionsUI.gameState.player.place.resources
                                    )
                                }
                            })
                        }

                        "JoinMeeting" ->
                        {
                            this.setDrawable(defaultSkin, "ChatGrunge")
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

                        "JoinConference" ->
                        {
                            this.setDrawable(defaultSkin, "ChatGrunge")
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        ).also {
                                            it.meetingName =
                                                this@AvailableActionsUI.gameState.ongoingMeetings.filter {
                                                    it.value.scheduledCharacters.contains(this@AvailableActionsUI.gameState.playerName) && it.value.place == this@AvailableActionsUI.gameState.player.place.name
                                                }.keys.first()
                                        }
                                    )
                                }
                            })
                        }

                        "NewAgenda" ->
                        {
                            this.setDrawable(defaultSkin, "PlusGrunge")
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
                            this.setDrawable(defaultSkin, "XGrunge")
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
                                            this@AvailableActionsUI.gameState.playerName,
                                            this@AvailableActionsUI.gameState.player.place.name
                                        )

                                    )
                                }
                            })
                        }
                        //TODO: also make changes to NewAgendaUI.kt.
                        else ->
                        {
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

    companion object
    {
        var actionCallbackIntercept: ((GameAction) -> Unit)? = null

        //Singleton
        fun gameActionCallback(action: GameAction)
        {
            if (actionCallbackIntercept != null)
            {
                actionCallbackIntercept!!(action)
                return
            }
            GameEngine.acquireCallback(action)
        }
    }


}