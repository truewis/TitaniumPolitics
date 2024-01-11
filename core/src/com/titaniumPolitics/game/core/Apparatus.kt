package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
class Apparatus {
    var name = ""
    var durability = 0
        set(value) {
            field = when {
                value > 100 -> 100
                value < 0 -> 0
                else -> value
            }
        }
    var baseDanger = .0
    var idealAbsorption = hashMapOf<String, Int>()
    var idealProduction = hashMapOf<String, Int>()
    var idealConsumption = hashMapOf<String, Int>()
    var idealDistribution = hashMapOf<String, Int>() //Converts resources into market resources.
    var idealWorker = 0
    var currentWorker = 0
    val currentProduction: Map<String, Int> get(){
        val result = hashMapOf<String, Int>()
        idealProduction.forEach {
            if(idealWorker==0)return result//No production if no worker.
            if(durability==0) return result//No production if broken.
            if(currentWorker<=idealWorker)
            result[it.key] = it.value * currentWorker/idealWorker
            else
            result[it.key] = it.value+it.value*(currentWorker-idealWorker)/idealWorker/2 //Labor efficiency drops to 50% if overcrowded.

        }
        return result
    }
    val currentConsumption: Map<String, Int> get(){
        val result = hashMapOf<String, Int>()
        idealConsumption.forEach {
            if(idealWorker==0)return result//No production if no worker.
            if(durability==0) return result//No production if broken.
            if(currentWorker<=idealWorker)
                result[it.key] = it.value * currentWorker/idealWorker
            else
                result[it.key] = it.value+it.value*(currentWorker-idealWorker)/idealWorker/2 //Labor efficiency drops to 50% if overcrowded.

        }
        return result
    }
    val currentAbsorption: Map<String, Int> get(){
        val result = hashMapOf<String, Int>()
        idealAbsorption.forEach {
            if(idealWorker==0)return result//No production if no worker.
            if(durability==0) return result//No production if broken.
            if(currentWorker<=idealWorker)
                result[it.key] = it.value * currentWorker/idealWorker
            else
                result[it.key] = it.value+it.value*(currentWorker-idealWorker)/idealWorker/2 //Labor efficiency drops to 50% if overcrowded.

        }
        return result
    }
    val currentDistribution: Map<String, Int> get(){
        val result = hashMapOf<String, Int>()
        idealDistribution.forEach {
            if(idealWorker==0)return result//No production if no worker.
            if(durability==0) return result//No production if broken.
            if(currentWorker<=idealWorker)
                result[it.key] = it.value * currentWorker/idealWorker
            else
                result[it.key] = it.value+it.value*(currentWorker-idealWorker)/idealWorker/2 //Labor efficiency drops to 50% if overcrowded.

        }
        return result
    }
    val currentDanger: Double get(){
        return if(currentWorker==0) 0.0 else if(durability==0) 0.0 else {
            if (currentWorker <= idealWorker)
                baseDanger * (2 - currentWorker / idealWorker) * 100/durability
            else
                baseDanger * (2 * currentWorker / idealWorker - 1) * 100/durability //Danger increases when overcrewed or undercrewed.
        }
    }
    val currentGraveDanger: Double get(){
        return if(currentWorker==0) 0.0
        else if(durability==0) 0.0
        else if(currentWorker<=idealWorker*4/5)
            baseDanger * (0.2-currentWorker/4/idealWorker)* 100/durability
        else if(currentWorker>=idealWorker*6/5)
            baseDanger * (2*currentWorker/3/idealWorker-0.8) * 100/durability //Nonzero only when very overcrewed or undercrewed.
        else
            0.0
    }

    override fun toString(): String {
        return "Apparatus(name='$name', durability=$durability, baseDanger=$baseDanger, idealProduction=$idealProduction, idealWorker=$idealWorker, currentWorker=$currentWorker, currentProduction=$currentProduction, currentDanger=$currentDanger, currentGraveDanger=$currentGraveDanger)"
    }

}