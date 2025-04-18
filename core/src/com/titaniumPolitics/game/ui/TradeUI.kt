package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.Trade
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class TradeUI(var gameState: GameState) : Table(defaultSkin)
{
    var titleLabel: Label
    private val docList1 = VerticalGroup()
    private val docList2 = VerticalGroup()

    //val submitButton = TextButton("지시", skin)
    //val cancelButton = TextButton("취소", skin)
    var trade: Trade = Trade("", "")

    init
    {
        instance = this
        titleLabel = Label("Trade", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val listScr1 = ScrollPane(docList1)
        docList1.grow()
        val listScr2 = ScrollPane(docList2)
        docList2.grow()

        add(listScr1).growY()
        add(listScr2).growY()
        row()
        //add(submitButton)
        //add(cancelButton)
        isVisible = false
//        GameEngine.acquireEvent += {
//            if (it.type == "Action")
//                submitButton.addListener(object : ClickListener()
//                {
//                    override fun clicked(event: InputEvent?, x: Float, y: Float)
//                    {
//                        super.clicked(event, x, y)
//                        GameEngine.acquireCallback(trade)
//                        isVisible = false
//                    }
//                })
//        }


    }

    fun open()
    {

        with(gameState) {
            val who =
                ongoingMeetings.filter { it.value.currentCharacters.contains(playerName) }
                    .flatMap { it.value.currentCharacters }.first { it != playerName }
            trade = Trade(playerName, player.place.name).apply { this.who = who }
            isVisible = true
            refreshList(
                player.resources,
                characters[who]!!.resources,
                informations.filter {
                    it.value.knownTo.contains(playerName)
                }
                    .map { it.key }.toHashSet(),
                informations.filter {
                    it.value.knownTo.contains(who) and !it.value.knownTo.contains(
                        playerName
                    )
                }.map { it.key }.toHashSet()
            )

        }
    }


    fun refreshList(
        items1: HashMap<String, Double>,
        items2: HashMap<String, Double>,
        info1: HashSet<String>,
        info2: HashSet<String>
    )
    {
        docList1.clear()
        docList2.clear()
        items1.forEach { tobj ->

            val t = scene2d.table {

                label(tobj.key, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                textField { text = "0" }.also { it2 ->
                    it2.addListener(object : ClickListener()
                    {
                        override fun clicked(event: InputEvent?, x: Float, y: Float)
                        {
                            super.clicked(event, x, y)
                            trade.item[tobj.key] = it2.text.toInt()
                        }
                    })
                }
                label("has: " + tobj.value.toString(), "trnsprtConsole") {
                    setFontScale(2f)
                }
            }
            docList1.addActor(t)
        }
        info1.forEach { tobj ->

            val t = scene2d.table {

                label(tobj, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                image(
                    (this@TradeUI.stage as CapsuleStage).assetManager.get(
                        "data/dev/capsuleDevBoxCheck.png",
                        Texture::class.java
                    )
                ) {
                    color = Color.GREEN
                    it.size(36f)
                }
            }
            docList1.addActor(t)
        }
        items2.forEach { tobj ->

            val t = scene2d.table {

                label(tobj.key, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                textField { text = "0" }.also { it2 ->
                    it2.addListener(object : ClickListener()
                    {
                        override fun clicked(event: InputEvent?, x: Float, y: Float)
                        {
                            super.clicked(event, x, y)
                            trade.item2[tobj.key] = it2.text.toInt()
                        }
                    })
                }
                label("has: " + tobj.value.toString(), "trnsprtConsole") {
                    setFontScale(2f)
                }
            }
            docList2.addActor(t)
        }
        info2.forEach { tobj ->

            val t = scene2d.table {

                label(tobj, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                image(
                    (this@TradeUI.stage as CapsuleStage).assetManager.get(
                        "data/dev/capsuleDevBoxCheck.png",
                        Texture::class.java
                    )
                ) {
                    color = Color.GREEN
                    it.size(36f)
                }
            }
            docList2.addActor(t)
        }
        //refresh the layout

        isVisible = true

    }

    companion object
    {
        lateinit var instance: TradeUI
    }


}