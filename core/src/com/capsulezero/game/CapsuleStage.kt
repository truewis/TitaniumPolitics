package com.capsulezero.game

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.capsulezero.game.core.GameState

class CapsuleStage(val gameState: GameState) : Stage(FitViewport(1920.0F, 1080.0F)) {

    //val inputEnabled = ArrayList<(Boolean)->Unit>() Unused
    val logBox = LogUI(gameState)
    val hud = HeadUpInterface(gameState)
    init {
        addActor(logBox)
        logBox.setFillParent(true)
        addActor(hud)
        hud.setFillParent(true)
    }


    override fun keyTyped(character: Char): Boolean {

        return super.keyTyped(character)
    }









}