package com.capsulezero.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState
import com.capsulezero.game.core.TradeParams
import ktx.scene2d.*

class TradeUI(skin: Skin?, var gameState: GameState) : Table(skin) {
    var titleLabel: Label
    private val docList1 = VerticalGroup()
    private val docList2 = VerticalGroup()
    private var isOpen = false;
    val submitButton = TextButton("지시", skin)
    val cancelButton = TextButton("취소", skin)
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
            if(it.type=="Trade")
            {
                refreshList(it.variables["items1"] as HashMap<String, Int>, it.variables["items2"] as HashMap<String, Int>, it.variables["info1"] as HashSet<String>, it.variables["info2"] as HashSet<String>)
            }
            submitButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    super.clicked(event, x, y)
                    GameEngine.acquireCallback(TradeParams(HashMap(), HashMap(), HashSet(), HashSet()))
                    isVisible = false
                }
            })
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
                textField { text = "0" }
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
                textField { text = "0" }
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