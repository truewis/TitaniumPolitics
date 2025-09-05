package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.AvailableActionsUI
import com.titaniumPolitics.game.ui.IActionUI
import ktx.scene2d.KTable
import ktx.scene2d.buttonGroup
import ktx.scene2d.scene2d

//Select action for e.g. request in this dialogue.
class ActionSelectUI(var gameState: GameState, override val actionCallback: (GameAction) -> Unit) :
    WindowUI("ActionSelectTitle"),
    KTable, IActionUI {
    private val docList = scene2d.buttonGroup(0, 1)
    override var subject: String = gameState.playerName
    override var tgtPlace: String = gameState.player.place.name
    private var _checkValidity: Boolean = false
    override fun setCheckValidity(checkValidity: Boolean) {
        _checkValidity = checkValidity
    }

    private val tgtPlaceObj get() = gameState.places[tgtPlace]!!
    private val sbjObject = gameState.characters[subject]!!

    private val actionDialogue = Container<Table>()
    var buttonOwner: ActionSelectButton? = null

    init {
        instance = this
        isVisible = false

        val docScr = ScrollPane(docList)
        docList.align(Align.center)
        with(content) {
            add(docScr).size(1200f, 150f).fill()
            row()
            add(actionDialogue).size(1200f, 800f).fill()
        }

    }

    fun refreshList(actionUIList: List<String>) {
        docList.clear()
        actionUIList.forEachIndexed { index, tobj ->
            val t = AvailableActionsUI.createActionButton(index, tobj, _checkValidity, gameState, {
                actionDialogue.actor = it
                (it as IActionUI).subject = subject
                (it as IActionUI).tgtPlace = tgtPlace
            }, actionCallback)
            docList.add(t).size(150f).fill()
        }

    }

    fun changeTgtPlace(placeName: String) {
        tgtPlace = placeName
    }

    companion object {
        lateinit var instance: ActionSelectUI
    }


}