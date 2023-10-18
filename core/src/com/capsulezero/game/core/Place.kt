package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Place : GameStateElement(){
    var name = ""
    var resources = hashMapOf<String, Int>()
    var connectedPlaces = arrayListOf<String>()
    var plannedWorker = 0
    val currentWorker :Int get() = apparatuses.sumOf { it.currentWorker }
    val maxResources: HashMap<String, Int> get() {
        val result = hashMapOf<String, Int>()
        result["water"]=0
        result["oxygen"]=0
        result["metal"]=0
        result["component"]=0
        result["ration"]=0
        apparatuses.forEach {
            if(it.durability!=0)
            when(it.name)
            {
                "waterStorage" -> result["water"] = result["water"]!! + 30000
                "oxygenStorage" -> result["oxygen"] = result["oxygen"]!! + 3000
                "metalStorage" -> result["metal"] = result["metal"]!! + 30000
                "componentStorage" -> result["component"] = result["component"]!! + 30000
                "rationStorage" -> result["ration"] = result["ration"]!! + 30000
            }
        }
        return result
    }

    var workHoursStart = 0
    var workHoursEnd=0
    var apparatuses = hashSetOf<Apparatus>()
    var characters = hashSetOf<String>()
    var responsibleParty = "" //Determines which party is responsible for the place.
    var isAccidentScene = false //If true, the place is closed and no one can enter. Can be cleared by clearAccidentScene.
    var accidentInformations = hashMapOf<String, Information>()//Information about the last accident. Non empty only when isAccidentScene is true.
    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        plannedWorker = apparatuses.sumOf { it.idealWorker }//TODO: this is a temporary solution to set up the planned worker.
        println("Planned worker of $name: $plannedWorker")
    }

}