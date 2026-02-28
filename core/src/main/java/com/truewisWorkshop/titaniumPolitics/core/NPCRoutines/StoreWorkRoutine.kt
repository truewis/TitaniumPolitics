package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.ProcessResource
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable

/**
 * Work routine for private business store employees (supplier, processor, server).
 *
 * - Leader (processor): stays at the store and converts ingredients into the luxury product
 *   when enough ingredients are available. Requires the corresponding trait (cook/jeweler/chemist).
 * - ADMINISTRATOR (supplier): roams the station to buy ingredients using BuyRoutine.
 * - TREASURER (server): stays at the store and examines resources so customers know what is in stock.
 */
@Serializable
class StoreWorkRoutine(val workplace: String) : Routine() {
    init {
        priority = PRIORITY_WORK
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (!isWorkCondition(name, place, workplace, gState)) return success()

        val character = gState.characters[name]!!
        val storeParty = gState.places[workplace]?.workplaceParty ?: return success()

        // Handle meetings the character is forced into.
        if (character.currentMeeting != null) {
            if (subroutines.none {
                    it is MeetingRoutine && it.meetingName == gState.meetingName(character.currentMeeting!!)
                }
            )
                return AttendPrivateMeetingRoutine(
                    scheduledMeetingName = gState.meetingName(character.currentMeeting!!)
                ).apply { priority = PRIORITY_MEETING }
        }

        when (storeParty.getRole(name)) {
            Party.Role.ADMINISTRATOR -> {
                // Supplier: buy ingredients when the store is running low.
                val ingredients = storeIngredients(workplace) ?: return null
                val batchesAvailable = ingredients.keys.minOfOrNull { key ->
                    gState.places[workplace]!!.resources[key] / ingredients[key]
                } ?: 0.0
                if (batchesAvailable < MIN_INGREDIENT_BATCHES && subroutines.none { it is BuyRoutine }) {
                    val neededIngredient = ingredients.keys.minByOrNull { key ->
                        gState.places[workplace]!!.resources[key] / (ingredients[key] + DIVISION_EPSILON)
                    }!!
                    val buyAmount = ingredients[neededIngredient] * TARGET_INGREDIENT_BATCHES
                    return BuyRoutine(neededIngredient, buyAmount)
                }
            }

            Party.Role.TREASURER -> {
                // Server: stay at the store. Move there if not already there.
                if (place != workplace && subroutines.none { it is MoveRoutine }) {
                    return MoveRoutine(workplace)
                }
            }

            Party.Role.NONE, Party.Role.OVERSEER -> {
                // Leader/overseer (processor): stay at the store to process ingredients.
                if (place != workplace && subroutines.none { it is MoveRoutine }) {
                    return MoveRoutine(workplace)
                }
            }

            else -> {}
        }

        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val storeParty = gState.places[workplace]?.workplaceParty

        // Leader/overseer: try to process ingredients into the product.
        if ((storeParty?.leader == name || storeParty?.overseer == name) && place == workplace) {
            val ingredients = storeIngredients(workplace)
            val output = storeOutput(workplace)
            if (ingredients != null && output != null) {
                ProcessResource(name, place, ingredients, output, gState).also {
                    if (it.isValid()) return it
                }
            }
        }

        // Server (TREASURER): examine resources so customers can learn what is in stock.
        if (storeParty?.treasurer == name && place == workplace) {
            return Examine(name, place, InformationType.RESOURCES, gState)
        }

        return Wait(name, place, gState)
    }

    companion object {
        /** Minimum batches of ingredients that trigger the supplier to restock. */
        const val MIN_INGREDIENT_BATCHES = 3.0

        /** Target batches to buy when restocking. */
        const val TARGET_INGREDIENT_BATCHES = 10.0

        /** Small value added to ingredient amounts to prevent division by zero when calculating batches. */
        const val DIVISION_EPSILON = 1e-12

        /** Store names that are private businesses, mapped to the product they sell. */
        private val STORE_PRODUCT_MAP = mapOf(
            "restaurant" to "fineFood",
            "jewelryShop" to "diamond",
            "ammoniaShop" to "ammonia",
        )

        /** Input ingredients for each store type (consumed when processing). */
        private val STORE_INGREDIENT_MAP = mapOf(
            "restaurant" to Resources("ration" to 3.0, "water" to 2.0),
            "jewelryShop" to Resources("rareMetal" to 0.01),
            "ammoniaShop" to Resources("water" to 5.0, "hydrogen" to 1.0),
        )

        /** Output resources produced by processing in each store type. */
        private val STORE_OUTPUT_MAP = mapOf(
            "restaurant" to Resources("fineFood" to 1.0),
            "jewelryShop" to Resources("diamond" to 0.001),
            "ammoniaShop" to Resources("ammonia" to 1.0),
        )

        fun storeProduct(storeName: String): String? = STORE_PRODUCT_MAP[storeName]

        fun storeIngredients(storeName: String): Resources? = STORE_INGREDIENT_MAP[storeName]

        fun storeOutput(storeName: String): Resources? = STORE_OUTPUT_MAP[storeName]

        /** Returns true if the given place is a private store managed by this routine. */
        fun isPrivateStore(storeName: String): Boolean = storeName in STORE_PRODUCT_MAP
    }
}
