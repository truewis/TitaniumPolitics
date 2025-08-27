package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_AmmoniaRepairDelay : EventObject("Merchant offers bribe to delay ammonia apparatus repair.", false)
{
    // Track which apparatus this event is currently monitoring
    @Transient
    var triggeredApparatusName: String? = null
    
    @Transient
    var triggeredPlaceName: String? = null
    
    @Transient
    var merchantOfferTime: Int = -1
    
    @Transient
    var merchantOfferAccepted: Boolean = false
    
    @Transient
    var bribeAmount: Int = 50
    
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
    }

    @Transient
    val func = { _: Int, newTime: Int ->
        // Only check once per time step to avoid multiple triggers
        if (triggeredApparatusName == null) {
            // Look for any apparatus with ammonia production that has durability = 0
            parent.places.forEach { (placeName, place) ->
                place.apparatuses.forEach { apparatus ->
                    if (apparatus.durability == 0 && apparatus.idealProduction.containsKey("ammonia")) {
                        // Found a broken ammonia apparatus, trigger merchant event
                        triggeredApparatusName = apparatus.name
                        triggeredPlaceName = placeName
                        merchantOfferTime = newTime
                        
                        // Give the player a bribe if Rui is at the location
                        if (parent.player.place.name == placeName) {
                            parent.player.resources["water"] = (parent.player.resources["water"] ?: 0) + bribeAmount
                            merchantOfferAccepted = true
                            
                            // Display dialogue about merchant offer
                            // Note: In a real implementation, this would trigger a dialogue choice
                            println("A merchant approaches Rui at $placeName and offers a bribe of $bribeAmount water to delay repair of the $triggeredApparatusName.")
                            
                            // For now, auto-accept the bribe
                            println("Rui accepts the merchant's bribe.")
                        }
                        return@forEach
                    }
                }
            }
        } else {
            // Check if apparatus has been repaired
            val place = parent.places[triggeredPlaceName]
            val apparatus = place?.apparatuses?.find { it.name == triggeredApparatusName }
            
            if (apparatus != null && apparatus.durability > 0) {
                // Apparatus was repaired, check timing
                val timeSinceOffer = newTime - merchantOfferTime
                val daysSinceOffer = timeSinceOffer / ReadOnly.const("lengthOfDay").toInt()
                
                if (daysSinceOffer < 2) {
                    // Repaired before 2 days, merchant loses mutuality with Rui
                    parent.setMutuality("merchant", "Rui", -10.0)
                    println("The merchant is displeased that Rui repaired the apparatus too quickly. Merchant's relationship with Rui decreases.")
                } else {
                    // Waited 2+ days, give additional rewards
                    parent.player.resources["water"] = (parent.player.resources["water"] ?: 0) + bribeAmount
                    parent.player.resources["ration"] = (parent.player.resources["ration"] ?: 0) + 20
                    parent.setMutuality("merchant", "Rui", 5.0)
                    println("The merchant is pleased that Rui delayed the repair. Additional rewards given!")
                }
                
                // Reset the event for next time
                resetEvent()
            } else if (apparatus == null) {
                // Apparatus was removed, reset event
                resetEvent()
            }
        }
    }
    
    private fun resetEvent() {
        triggeredApparatusName = null
        triggeredPlaceName = null
        merchantOfferTime = -1
        merchantOfferAccepted = false
    }

    override fun activate()
    {
        parent.timeChanged += func
    }

    override fun deactivate()
    {
        parent.timeChanged -= func
    }
}