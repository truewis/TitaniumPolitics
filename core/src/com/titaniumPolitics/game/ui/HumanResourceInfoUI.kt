package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.Place

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

//Human Resource Management is currently done without information. The report is instant.
class HumanResourceInfoUI : Table(defaultSkin), KTable
{
    private val dataTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        stack {
            it.grow()
            image("capsuleDevLabel1") {
            }
            add(informationPane)

        }


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