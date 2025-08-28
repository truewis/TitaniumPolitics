package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class StartMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val targetMeeting
        get() =
            parent.scheduledMeetings.filter {
                it.value.isValidTimeToStart(parent.time)
                        && it.value.place == tgtPlace
            }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(sbjCharacter) }.keys.firstOrNull()

    override fun chooseParams() {

    }

    //Also refer to Talk.execute()
    override fun execute() {
        val oldTgtMeeting = this.targetMeeting
        parent.addOngoingMeeting(parent.scheduledMeetings[oldTgtMeeting]!!)
        parent.removeScheduledMeeting(oldTgtMeeting!!)
        parent.ongoingMeetings[oldTgtMeeting]!!.currentCharacters.add(sbjCharacter)
        // Interrupt other required characters and add them to the meeting.
        val meeting = parent.ongoingMeetings[oldTgtMeeting]!!
        meeting.currentSpeaker = sbjCharacter
        meeting.currentAttention = (sbjCharObj.will * sbjCharObj.stats.pScale).toInt()
        meeting.startTime = parent.time
        val requiredCharacters = meeting.scheduledCharacters.intersect(tgtPlaceObj.characters)
            .filter { s -> parent.characters[s]!!.currentMeeting == null /*Forcing characters out of meetings causes bunch of problems, such as missing speaker. Don't do this.*/ }
        requiredCharacters.forEach {
            parent.characters[it]!!.frozen = 1 //Force them to join the meeting.
            parent.ongoingMeetings[oldTgtMeeting]!!.currentCharacters.add(it)
            Logger.write(
                "Interrupt: $it is forced to join by $sbjCharacter starting the meeting.",
                Logger.LogLevel.INFO
            )
        }
        Logger.write(
            "Meeting $oldTgtMeeting started by $sbjCharacter at ${parent.time} in $tgtPlace.",
            Logger.LogLevel.INFO
        )
        super.execute()

    }

    override fun isValid(): Boolean {
        if (sbjCharObj.currentMeeting != null) {
            Logger.write(
                "Cannot start a meeting $targetMeeting while already in one: ${sbjCharObj.currentMeeting}",
                Logger.LogLevel.ERROR
            )
            return false
        }
        return targetMeeting != null &&
                //Check if there are enough characters scheduled to attend the meeting.
                parent.scheduledMeetings[targetMeeting]!!.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters.filter { s -> parent.characters[s]!!.currentMeeting == null }).size >= 2 &&
                (parent.scheduledMeetings[targetMeeting]!!.type != Meeting.MeetingType.DIVISION_LEADER_ELECTION || //Beware that division leader elections can only be started by the controller.
                        sbjCharacter == "ctrler")

        //NOTICE: The subject character need not be the leader of the party. This way meetings are more flexible and can be initiated by any character who is scheduled to attend the meeting.
    }

}