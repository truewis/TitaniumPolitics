package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.*
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

//Select action for e.g. request in this dialogue.
class ActionSelectUI(var gameState: GameState, override var actionCallback: (GameAction) -> Unit) : Table(defaultSkin),
    KTable, ActionUI
{
    private val docList = scene2d.buttonGroup(0, 1)
    private var subject = gameState.playerName
    private var tgtPlace = gameState.player.place.name
    private val tgtPlaceObj = gameState.places[tgtPlace]!!
    private val sbjObject = gameState.characters[subject]!!

    private val actionDialogue = Container<Table>()
    val options: ExamineUI

    init
    {


        options = ExamineUI(this@ActionSelectUI.gameState)
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

        add(docScr).size(1200f, 150f).fill()
        row()
        add(actionDialogue).size(1200f, 800f).fill()
        refreshList(listOf("UnofficialResourceTransfer", "OfficialResourceTransfer"))
    }

    fun refreshList(actionUIList: List<String>)
    {
        docList.clear()
        actionUIList.forEach { tobj ->
            val table: WindowUI = when (tobj)
            {
                "NewAgenda" -> NewAgendaUI(gameState, actionCallback)
                "OfficialResourceTransfer" -> ResourceTransferUI(gameState, actionCallback)
                "UnofficialResourceTransfer" -> ResourceTransferUI(gameState, actionCallback).also {
                    it.mode = "unofficial"
                }

                else -> NewAgendaUI(
                    gameState,
                    actionCallback
                ).also { println("Warning: The ActionUI for $tobj is not found.") }
            }
            (table as ActionUI).changeSubject(subject)

            actionDialogue.actor = table
            val t = scene2d.button {
                addListener(ActionTooltipUI(tobj))
                container {
                    it.size(150f)
                    it.fill(0.66f, 0.66f)
                    it.align(Align.center)
                    image("Help") {


                        when (tobj)
                        {


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
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    )
                                    {
                                        with(table as ResourceTransferUI) {
                                            isVisible = true
                                            refresh(
                                                "unofficial",
                                                actionCallback,
                                                this@ActionSelectUI.tgtPlaceObj.resources.toHashMap()
                                            )
                                        }

                                    }
                                })
                            }

                            "OfficialResourceTransfer" ->
                            {
                                this.setDrawable(defaultSkin, "TilesGrunge")
                                this@button.addListener(object : ClickListener()
                                {
                                    override fun clicked(
                                        event: InputEvent?,
                                        x: Float,
                                        y: Float
                                    )
                                    {
                                        with(table as ResourceTransferUI) {
                                            isVisible = true
                                            refresh(
                                                "official",
                                                actionCallback,
                                                this@ActionSelectUI.tgtPlaceObj.resources.toHashMap()
                                            )
                                        }
                                    }
                                })
                            }
//TODO: Recursive UI creation. Fix this.
//                            "NewAgenda" ->
//                            {
//                                this.setDrawable(defaultSkin, "PlusGrunge")
//                                this@button.addListener(object : ClickListener()
//                                {
//                                    override fun clicked(
//                                        event: InputEvent?,
//                                        x: Float,
//                                        y: Float
//                                    )
//                                    {
//                                        with(table as NewAgendaUI) {
//                                            isVisible = true
//                                            refresh(
//                                                this@ActionSelectUI.gameState
//                                            )
//                                        }
//                                    }
//                                })
//                            }

                            //TODO: also make changes to NewAgendaUI.kt.
                            else ->
                            {
                                this.setDrawable(defaultSkin, "Help")

                            }
                        }

                    }
                }
            }
            table.onClose += {
                //TODO: When the player press the submit button of the actionUI, the action is ready to go.
                //docList.buttonGroup.uncheckAll()
            }
            docList.addActor(t)
        }
        isVisible = !docList.children.isEmpty

    }

    override fun changeSubject(charName: String)
    {
        subject = charName
        tgtPlace = sbjObject.place.name
    }

    fun changeTgtPlace(placeName: String)
    {
        tgtPlace = placeName
    }


}