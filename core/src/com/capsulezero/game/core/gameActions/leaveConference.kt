package com.capsulezero.game.core.gameActions

class leaveConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {

    override fun execute() {
        val meetingName = parent.ongoingConferences.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.first()
        parent.ongoingConferences[meetingName]!!.currentCharacters.remove(tgtCharacter)
        if(parent.ongoingConferences[meetingName]!!.currentCharacters.count()==0 || parent.characters[tgtCharacter]!!.trait.contains("mechanic")) {
            parent.ongoingConferences.remove(meetingName)//End the meeting if it has no participants, or if the mechanic leaves

        }
    }

}