package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.math.exp

@Serializable
class Place : GameStateElement()
{
    override val name: String
        get() = parent.places.filter { it.value == this }.keys.first()
    var resources = hashMapOf<String, Double>()
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
    var gasPressure = hashMapOf<String, Double>("oxygen" to 20000.0, "carbonDioxide" to 100.0)
    var connectedPlaces = arrayListOf<String>()
    var plannedWorker = 0
    var coordinates = Coordinate3D(0, 0, 0)
    var temperature = 300 //Ambient temperature in Kelvin.
    var volume = 1000f //Volume in m^3.
    val currentWorker: Int get() = apparatuses.sumOf { it.currentWorker }
    val maxResources: HashMap<String, Double>
        get()
        {
            val result = hashMapOf<String, Double>()
            apparatuses.forEach {
                if (it.durability > .0)
                    when (it.name)
                    {
                        "waterStorage" -> result["water"] = (result["water"] ?: .0) + 30000
                        "oxygenStorage" -> result["oxygen"] = (result["oxygen"] ?: .0) + 3000
                        "metalStorage" -> result["metal"] = (result["metal"] ?: .0) + 30000
                        "componentStorage" -> result["component"] = (result["component"] ?: .0) + 30000
                        "rationStorage" -> result["ration"] = (result["ration"] ?: .0) + 30000
                        else ->//concatenate string
                        {
                            if (it.name.contains("Storage"))
                            {
                                val resource = it.name.substringBefore("Storage").lowercase(Locale.getDefault())
                                result[resource] = (result[resource] ?: .0) + 30000
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
    var accidentInformationKeys =
        hashSetOf<String>()//Information about the last accident. Non empty only when isAccidentScene is true.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        plannedWorker =
            apparatuses.sumOf { it.idealWorker }//TODO: this is a temporary solution to set up the planned worker. It should be set by division leaders.
    }

    //Check the gas pressure of the connected places and slowly equalize it. This function is called every time change.
    fun diffuseGas()
    {
        connectedPlaces.forEach {
            val place = parent.places[it]!!
            //For each gas type, use the coordinates and the density in gasJson to distribute the gas according to the boltzmann distribution.
            gasPressure.forEach { (key, _) ->
                val mass =
                    (ReadOnly.gasJson[key]!!.jsonObject["density"]!!.jsonPrimitive.float) * 22.4f / ReadOnly.NA
                val potentialDiff = coordinates.z - place.coordinates.z
                val ratio = exp(
                    -(ReadOnly.GA * mass * potentialDiff) / (ReadOnly.KB * temperature) //[J] = [kg*m^2/s^2]
                ) //Boltzmann distribution. TODO: reflect the volume of the place.
                val equilabriumGasAmount =
                    ((gasPressure[key] ?: .0) * volume + (place.gasPressure[key]
                        ?: .0) * place.volume) / (volume + ratio * place.volume)

                val flowAmount =
                    (equilabriumGasAmount - (gasPressure[key] ?: .0) * volume) * dt / const("GasDiffusionTau")

                gasPressure[key] =
                    (gasPressure[key] ?: .0) + flowAmount / volume

                place.gasPressure[key] =
                    (place.gasPressure[key] ?: .0) - flowAmount / place.volume
            }
        }


    }

}