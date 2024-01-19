package com.titaniumPolitics.game.core.gameActions

class chat(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    override fun execute()
    {
        //TODO: acquire or give whole information to another character by chance.
        val who = parent.ongoingMeetings.filter { it.value.currentCharacters.contains(tgtCharacter) }.flatMap { it.value.currentCharacters }.filter { it != tgtCharacter }.toHashSet()


        var count = 0


        val factor = .1
        //Mutualities between this character and all other characters in the meeting/conference increase by the number of information exchanged + 1.
        who.forEach {
            parent.setMutuality(tgtCharacter, it, 1.0 + count * factor)
            parent.setMutuality(it, tgtCharacter, 1.0 + count * factor)
        }

        //if (tgtCharacter == who){ println("You chat with yourself.");return}
        parent.characters[tgtCharacter]!!.frozen++
    }

}