package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information

import ktx.scene2d.*


class ResourceInfoUI(skin: Skin?, var gameState: GameState) : Table(skin), KTable
{
    init
    {
        debug()

// Assuming you have an instance of Information like this
        val information = Information(
            author = "Author",
            creationTime = 0,
            type = "resources",
            tgtTime = 0,
            tgtPlace = "Place",
            tgtResource = "ration",
            amount = 10
        ).also { it.knownTo.add("Author"); it.credibility = 100 }

        val dataTable = scene2d.table {
            label("Resource") {
                it.growX()
                setAlignment(Align.center)
            }
            label("Amount") {
                it.growX()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }
            row()

            label(information.tgtResource) {
                it.fill()
                setAlignment(Align.center)
            }
            label(information.amount.toString()) {
                it.fill()
                setAlignment(Align.center)
            }

            setFillParent(true)
        }


        val authorLabel = label("Author: ${information.author}") { setAlignment(Align.center) }
        row()
        val creationTimeLabel = label("Creation Time: ${information.creationTime}") { setAlignment(Align.center) }
        row()
        val typeLabel = label("Type: ${information.type}") { setAlignment(Align.center) }
        row()
        val tgtTimeLabel = label("Target Time: ${information.tgtTime}") { setAlignment(Align.center) }
        row()
        val tgtPlaceLabel = label("Target Place: ${information.tgtPlace}") { setAlignment(Align.center) }
        row()
        val scrollPane = scrollPane {
            it.grow()
            addActor(dataTable)
            setScrollingDisabled(true, false)
        }


    }


}