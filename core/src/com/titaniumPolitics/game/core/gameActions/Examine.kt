package com.titaniumPolitics.game.core.gameActions

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.ui.HumanResourceInfoUI
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Serializable
class Examine(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var what = ""
    override fun chooseParams()
    {
        what = GameEngine.acquire(arrayListOf("HR", "apparatus", "resources"))
    }

    override fun execute()
    {
        when (what)
        {
            "HR" ->
            {
                //Acquire HR information is not planned.
                println("HR: ${parent.places[tgtPlace]!!.currentWorker}/${parent.places[tgtPlace]!!.plannedWorker}, ${parent.places[tgtPlace]!!.workHoursStart}-${parent.places[tgtPlace]!!.workHoursEnd}, ${parent.places[tgtPlace]!!.responsibleParty}")
                //This action has no effect on the game state.
                //Open HR window Directly.
                if (sbjCharacter == parent.playerName)
                {
                    runBlocking {
                        suspendCoroutine { cont ->
                            Gdx.app.postRunnable {
                                HumanResourceInfoUI.instance.isVisible = true
                                HumanResourceInfoUI.instance.refresh(parent.places[tgtPlace]!!, parent.time)
                                cont.resume(Unit)
                            }
                        }
                    }
                }
            }

            "apparatus" ->
            {
                println("Apparatus: ${parent.places[tgtPlace]!!.apparatuses}")

                //Acquire apparatus information.
                parent.places[tgtPlace]!!.apparatuses.forEach { entry ->
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.APPARATUS_DURABILITY,
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        tgtApparatus = entry.name,
                        amount = entry.durability
                    ).also {
                        it.knownTo.add(sbjCharacter);parent.informations[it.generateName()] = it
                    }

                }
            }

            "resources" ->
            {
                if (tgtPlace.contains("home"))
                {//Home is the exception; character's resources are shown instead.
                    println("Resources: ${parent.characters[sbjCharacter]!!.resources}")
                    //Acquire resources information of this character.
                    parent.characters[sbjCharacter]!!.resources
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.RESOURCES,
                        tgtTime = parent.time,
                        tgtCharacter = sbjCharacter,
                        resources = parent.characters[sbjCharacter]!!.resources
                    ).also {
                        it.knownTo.add(sbjCharacter);parent.informations[it.generateName()] = it
                    }

                } else
                {
                    println("Resources: ${parent.places[tgtPlace]!!.resources}")
                    //Acquire resources information of this place.
                    Information(
                        author = sbjCharacter,
                        creationTime = parent.time,
                        type = InformationType.RESOURCES,
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        resources = parent.places[tgtPlace]!!.resources
                    ).also {
                        it.knownTo.add(sbjCharacter);parent.informations[it.generateName()] =
                        it
                    }

                }

            }
        }
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return true
    }

    override fun deltaWill(): Double
    {
        var w = super.deltaWill()
        if (parent.characters[sbjCharacter]!!.trait.contains("investigator"))
            w += 10
        return w
    }

}