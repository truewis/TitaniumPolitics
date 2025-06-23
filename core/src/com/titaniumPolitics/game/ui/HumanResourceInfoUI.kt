package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.ui.widget.WindowUI

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

//Human Resource Management is currently done without information. The report is instant.
class HumanResourceInfoUI : Table(defaultSkin) {
    private val dataTable = Table()

    init {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        add(informationPane).grow()


    }

    fun refresh(place: Place, reportTime: Int) {
        dataTable.clear()
        dataTable.apply {

            add(scene2d.label("Report Time: $reportTime") { setAlignment(Align.center) })

            row()
            add(scene2d.label("Work Hours: ${place.workHoursStart} - ${place.workHoursEnd}") { setAlignment(Align.center) })
            row()
            add(scene2d.label("Planned Workers: ${place.plannedWorker}") { setAlignment(Align.center) })
            row()
            add(scene2d.label("Current Workers: ${place.currentWorker}") { setAlignment(Align.center) })
            row()
            add(scene2d.label("Ideal Workers: ${place.apparatuses.sumOf { it.idealWorker }}") { setAlignment(Align.center) })
        }

    }

    companion object {
        //Singleton
        lateinit var instance: HumanResourceInfoUI
    }


}