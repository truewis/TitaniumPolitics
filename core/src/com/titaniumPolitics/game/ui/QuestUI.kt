package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.ClockUI.Companion.formatTime
import ktx.scene2d.*

class QuestUI(var gameState: GameState) : Table(Scene2DSkin.defaultSkin)
{
    var titleLabel: Label
    private val docList = VerticalGroup()
    private var isOpen = false;

    init
    {
        titleLabel = Label("Quest:", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.quests.newItemAdded += { Gdx.app.postRunnable { refreshList(); } }
        gameState.quests.expired += { Gdx.app.postRunnable { refreshList(); } }
        gameState.quests.completed += { Gdx.app.postRunnable { refreshList(); } }
        gameState.timeChanged += { _, _ -> Gdx.app.postRunnable { refreshList(); } }
    }


    fun refreshList()
    {
        docList.clear()
        gameState.quests.dataBase.forEach { tobj ->
            if (tobj.due != 0 && tobj.due + 1 < gameState.time) return@forEach
            if (tobj.completed != 0 && tobj.completed + 1 < gameState.time) return@forEach
            val t = scene2d.table {
                if (tobj.completed != 0) image(
                    (this@QuestUI.stage as CapsuleStage).assetManager.get(
                        "data/dev/capsuleDevBoxCheck.png",
                        Texture::class.java
                    )
                ) {
                    color = Color.GREEN
                    it.size(36f)
                } else image(
                    (this@QuestUI.stage as CapsuleStage).assetManager.get(
                        "data/dev/capsuleDevBox.png",
                        Texture::class.java
                    )
                ) {
                    color = Color.GREEN
                    it.size(36f)
                }
                label(tobj.name, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                if (tobj.due != 0 && tobj.completed == 0)
                {
                    label(formatTime(tobj.due), "trnsprtConsole") {
                        if (tobj.due < gameState.time) color = Color.RED
                        setFontScale(2f)
                        it.width(150f)
                    }
                    val l = Label(formatTime(tobj.due), skin, "trnsprtConsole")
                    if (tobj.due < gameState.time) l.color = Color.RED
                    l.setFontScale(2f)
                }
            }
            docList.addActor(t)
        }
        isVisible = !docList.children.isEmpty

    }


}