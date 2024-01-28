package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information

class examine(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
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
            }

            "apparatus" ->
            {
                println("Apparatus: ${parent.places[tgtPlace]!!.apparatuses}")

                //Acquire apparatus information.
                parent.places[tgtPlace]!!.apparatuses.forEach { entry ->
                    Information(
                        author = tgtCharacter,
                        creationTime = parent.time,
                        type = "apparatusDurability",
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        tgtApparatus = entry.name,
                        amount = entry.durability
                    ).also {
                        it.knownTo.add(tgtCharacter);it.credibility = 100;parent.informations[it.generateName()] = it
                    }

                }
            }

            "resources" ->
            {
                if (tgtPlace == "home")
                {//Home is the exception; character's resources are shown instead.
                    println("Resources: ${parent.characters[tgtCharacter]!!.resources}")
                    //Acquire resources information of this character.
                    parent.characters[tgtCharacter]!!.resources
                    Information(
                        author = tgtCharacter,
                        creationTime = parent.time,
                        type = "resources",
                        tgtTime = parent.time,
                        tgtCharacter = tgtCharacter,
                        resources = parent.characters[tgtCharacter]!!.resources
                    ).also {
                        it.knownTo.add(tgtCharacter);it.credibility = 100;parent.informations[it.generateName()] = it
                    }

                } else
                {
                    println("Resources: ${parent.places[tgtPlace]!!.resources}")
                    //Acquire resources information of this place.
                    Information(
                        author = tgtCharacter,
                        creationTime = parent.time,
                        type = "resources",
                        tgtTime = parent.time,
                        tgtPlace = tgtPlace,
                        resources = parent.places[tgtPlace]!!.resources
                    ).also {
                        it.knownTo.add(tgtCharacter);it.credibility = 100;parent.informations[it.generateName()] =
                        it
                    }

                }

            }
        }
        parent.characters[tgtCharacter]!!.frozen += 2
    }

}