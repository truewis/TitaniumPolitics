//package com.capsulezero.game.ui
//
//import com.badlogic.gdx.graphics.Color
//import com.badlogic.gdx.graphics.Texture
//import com.badlogic.gdx.scenes.scene2d.ui.*
//import com.capsulezero.game.core.GameState
//import com.capsulezero.game.ui.Clock.Companion.formatTime
//import ktx.scene2d.image
//import ktx.scene2d.label
//import ktx.scene2d.scene2d
//import ktx.scene2d.table
//
//class TradeUI(skin: Skin?, var gameState: GameState) : Table(skin) {
//    var titleLabel: Label
//    private val docList = VerticalGroup()
//    private var isOpen = false;
//
//    init {
//        titleLabel = Label("거래", skin, "trnsprtConsole")
//        titleLabel.setFontScale(2f)
//        add(titleLabel).growX()
//        row()
//        val docScr = ScrollPane(docList)
//        docList.grow()
//
//        add(docScr).grow()
//        gameState.todo.newItemAdded += { refreshList(); }
//        gameState.todo.expired += { refreshList(); }
//        gameState.todo.completed += { refreshList(); }
//        gameState.timeChanged += { _, _ -> refreshList(); }
//    }
//
//
//    fun refreshList(items1: HashMap<String, Int>, items2: HashMap<String, Int>, info1: HashSet<String>, info2: HashSet<String>, action1: HashSet<String>, action2: HashSet<String>) {
//        docList.clear()
//        gameState.informations.filter { it.value.knownTo.contains(char1) and it.value.doesKnowExistence(char2) and !it.value.knownTo.contains(char2) }.forEach { tobj ->
//
//            val t = scene2d.table {
//                if (tobj.completed != 0) image((this@TradeUI.stage as CapsuleStage).assetManager.get("data/dev/capsuleDevBoxCheck.png", Texture::class.java)) {
//                    color = Color.GREEN
//                    it.size(36f)
//                } else image((this@TradeUI.stage as CapsuleStage).assetManager.get("data/dev/capsuleDevBox.png", Texture::class.java)) {
//                    color = Color.GREEN
//                    it.size(36f)
//                }
//                label(tobj.title, "trnsprtConsole") {
//                    it.growX()
//                    setFontScale(2f)
//                }
//                if (tobj.due != 0 && tobj.completed == 0) {
//                    label(formatTime(tobj.due), "trnsprtConsole") {
//                        if (tobj.due < gameState.time) color = Color.RED
//                        setFontScale(2f)
//                        it.width(150f)
//                    }
//                    val l = Label(formatTime(tobj.due), skin, "trnsprtConsole")
//                    if (tobj.due < gameState.time) l.color = Color.RED
//                    l.setFontScale(2f)
//                }
//            }
//            docList.addActor(t)
//        }
//        isVisible = !docList.children.isEmpty
//
//    }
//
//
//}