package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.trade
import ktx.scene2d.*

class TradeUI(skin: Skin?, var gameState: GameState) : Table(skin) {
    var titleLabel: Label
    private val docList1 = VerticalGroup()
    private val docList2 = VerticalGroup()
    private var isOpen = false;
    val submitButton = TextButton("지시", skin)
    val cancelButton = TextButton("취소", skin)
    var trade : trade = trade("", "")
    init {
        titleLabel = Label("거래", skin, "trnsprtConsole")
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
        add(submitButton)
        add(cancelButton)
        debug = true
        isVisible = false
        GameEngine.acquireEvent+={
            if(it.type=="Action")
            submitButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    GameEngine.acquireCallback(trade)
                    isVisible = false
                }
            })
        }


    }
    fun open() {

        with(gameState) {
            val who =
                ongoingMeetings.filter {it.value.currentCharacters.contains(playerAgent)}.flatMap { it.value.currentCharacters }.first {it!=playerAgent}
            trade = trade(playerAgent, characters[playerAgent]!!.place.name).apply { this.who = who }
            isVisible = true
            refreshList(characters[playerAgent]!!.resources,
                characters[who]!!.resources,
                informations.filter {
                    it.value.knownTo.contains(playerAgent)
                }
                    .map { it.key }.toHashSet(),
                informations.filter {
                    it.value.knownTo.contains(who) and !it.value.knownTo.contains(
                        playerAgent
                    )
                }.map { it.key }.toHashSet()
            )

        }
    }


    fun refreshList(items1: HashMap<String, Int>, items2: HashMap<String, Int>, info1: HashSet<String>, info2: HashSet<String>) {
        docList1.clear()
        docList2.clear()
        items1.forEach { tobj ->

            val t = scene2d.table {

                label(tobj.key, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                textField { text = "0" }.also { it2 ->
                    it2.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            trade.item[tobj.key] = it2.text.toInt()
                        }
                    })
                }
                label("has: "+tobj.value.toString(), "trnsprtConsole") {
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
                image((this@TradeUI.stage as CapsuleStage).assetManager.get("data/dev/capsuleDevBoxCheck.png", Texture::class.java)) {
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
                    it2.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            trade.item2[tobj.key] = it2.text.toInt()
                        }
                    })
                }
                label("has: "+tobj.value.toString(), "trnsprtConsole") {
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
                image((this@TradeUI.stage as CapsuleStage).assetManager.get("data/dev/capsuleDevBoxCheck.png", Texture::class.java)) {
                    color = Color.GREEN
                    it.size(36f)
                }
            }
            docList2.addActor(t)
        }
        //refresh the layout

        isVisible = true

    }


}