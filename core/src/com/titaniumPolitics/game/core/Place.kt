package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class Place : GameStateElement()
{
    override val name: String
        get() = parent.places.filter { it.value == this }.keys.first()
    var resources = hashMapOf<String, Int>()
        get()
        {
            //If the place is a home, return the resources of the character living there.
            if (name.contains("home_"))
                return parent.characters[name.substringAfter("home_")]!!.resources
            return field
        }
        set(value)
        {
            //If the place is a home, set the resources of the character living there.
            if (name.contains("home_"))
                parent.characters[name.substringAfter("home_")]!!.resources = value
            field = value
        }
    var connectedPlaces = arrayListOf<String>()
    var plannedWorker = 0
    var coordinates = Coordinate3D(0, 0, 0)
    val currentWorker: Int get() = apparatuses.sumOf { it.currentWorker }
    val maxResources: HashMap<String, Int>
        get()
        {
            val result = hashMapOf<String, Int>()
            apparatuses.forEach {
                if (it.durability != 0)
                    when (it.name)
                    {
                        "waterStorage" -> result["water"] = (result["water"] ?: 0) + 30000
                        "oxygenStorage" -> result["oxygen"] = (result["oxygen"] ?: 0) + 3000
                        "metalStorage" -> result["metal"] = (result["metal"] ?: 0) + 30000
                        "componentStorage" -> result["component"] = (result["component"] ?: 0) + 30000
                        "rationStorage" -> result["ration"] = (result["ration"] ?: 0) + 30000
                        else ->//concatenate string
                        {
                            if (it.name.contains("Storage"))
                            {
                                val resource = it.name.substringBefore("Storage").lowercase(Locale.getDefault())
                                result[resource] = (result[resource] ?: 0) + 30000
                            }
                        }
                    }
            }
            return result
        }

    var workHoursStart = 0
    var workHoursEnd = 0
    var apparatuses = hashSetOf<Apparatus>()
    var characters = hashSetOf<String>()
    var responsibleParty = "" //Determines which party is responsible for the place.
    var isAccidentScene =
        false //If true, the place is closed and no one can enter. Can be cleared by clearAccidentScene.
    var accidentInformations =
        hashMapOf<String, Information>()//Information about the last accident. Non empty only when isAccidentScene is true.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        plannedWorker =
            apparatuses.sumOf { it.idealWorker }//TODO: this is a temporary solution to set up the planned worker. It should be set by division leaders.
    }

}