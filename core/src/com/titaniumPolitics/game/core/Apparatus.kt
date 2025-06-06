package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

/* Apparatus is a kind of building that can be used to produce and consume resources.
* It can also be used to absorb resources from the environment.
* It can also be used to distribute resources to the market.
* It belongs to a place.
* It has a durability. When durability is 0, it is broken. When durability is 100, it is brand new.
* It has a danger. When danger is 0, it is safe. When danger is 100, it is very dangerous.
* Accidents may happen when danger is high.
*
* */
@Serializable
class Apparatus
{
    var name = ""
    var durability = 0
        set(value)
        {
            field = when
            {
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
    val currentProduction: Map<String, Int>
        get()
        {
            val result = hashMapOf<String, Int>()
            idealProduction.forEach {
                if (idealWorker == 0) return result//No production if no worker.
                if (durability == 0) return result//No production if broken.
                if (currentWorker <= idealWorker)
                    result[it.key] = it.value * currentWorker / idealWorker
                else
                    result[it.key] =
                        it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

            }
            return result
        }
    val currentConsumption: Map<String, Int>
        get()
        {
            val result = hashMapOf<String, Int>()
            idealConsumption.forEach {
                if (idealWorker == 0) return result//No production if no worker.
                if (durability == 0) return result//No production if broken.
                if (currentWorker <= idealWorker)
                    result[it.key] = it.value * currentWorker / idealWorker
                else
                    result[it.key] =
                        it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

            }
            return result
        }
    val currentAbsorption: Map<String, Int>
        get()
        {
            val result = hashMapOf<String, Int>()
            idealAbsorption.forEach {
                if (idealWorker == 0) return result//No production if no worker.
                if (durability == 0) return result//No production if broken.
                if (currentWorker <= idealWorker)
                    result[it.key] = it.value * currentWorker / idealWorker
                else
                    result[it.key] =
                        it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

            }
            return result
        }
    val currentDistribution: Map<String, Int>
        get()
        {
            val result = hashMapOf<String, Int>()
            idealDistribution.forEach {
                if (idealWorker == 0) return result//No production if no worker.
                if (durability == 0) return result//No production if broken.
                if (currentWorker <= idealWorker)
                    result[it.key] = it.value * currentWorker / idealWorker
                else
                    result[it.key] =
                        it.value + it.value * (currentWorker - idealWorker) / idealWorker / 2 //Labor efficiency drops to 50% if overcrowded.

            }
            return result
        }
    val currentDanger: Double
        get()
        {
            return if (currentWorker == 0) 0.0 else if (durability == 0) 0.0 else
            {
                if (currentWorker <= idealWorker)
                    baseDanger * (2 - currentWorker / idealWorker) * 100 / durability * ReadOnly.const("GlobalAccidentRate")
                else
                    baseDanger * (2 * currentWorker / idealWorker - 1) * 100 / durability * ReadOnly.const("GlobalAccidentRate")//Danger increases when overcrewed or undercrewed.
            }
        }
    val currentGraveDanger: Double
        get()
        {
            return if (currentWorker == 0) 0.0
            else if (durability == 0) 0.0
            else if (currentWorker <= idealWorker * 4 / 5)
                baseDanger * (0.2 - currentWorker / 4 / idealWorker) * 100 / durability * ReadOnly.const("GlobalAccidentRate")
            else if (currentWorker >= idealWorker * 6 / 5)
                baseDanger * (2 * currentWorker / 3 / idealWorker - 0.8) * 100 / durability * ReadOnly.const("GlobalAccidentRate") //Nonzero only when very overcrewed or undercrewed.
            else
                0.0
        }

    override fun toString(): String
    {
        return "Apparatus(name='$name', durability=$durability, baseDanger=$baseDanger, idealProduction=$idealProduction, idealWorker=$idealWorker, currentWorker=$currentWorker, currentProduction=$currentProduction, currentDanger=$currentDanger, currentGraveDanger=$currentGraveDanger)"
    }

}