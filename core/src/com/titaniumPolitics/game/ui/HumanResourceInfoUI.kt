package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.Place

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

//Human Resource Management is currently done without information. The report is instant.
class HumanResourceInfoUI : WindowUI("HumanResourceInfoTitle")
{
    private val dataTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()


    }

    fun refresh(place: Place, reportTime: Int)
    {
        dataTable.clear()
        dataTable.apply {

            add(label("Report Time: $reportTime") { setAlignment(Align.center) })

            row()
            add(label("Work Hours: ${place.workHoursStart} - ${place.workHoursEnd}") { setAlignment(Align.center) })
            row()
            add(label("Planned Workers: ${place.plannedWorker}") { setAlignment(Align.center) })
            row()
            add(label("Current Workers: ${place.currentWorker}") { setAlignment(Align.center) })
            row()
            add(label("Ideal Workers: ${place.apparatuses.sumOf { it.idealWorker }}") { setAlignment(Align.center) })
        }

    }

    companion object
    {
        //Singleton
        lateinit var instance: HumanResourceInfoUI
    }


}