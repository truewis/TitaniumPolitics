package com.titaniumPolitics.game.core.gameActions

@Deprecated("This class is deprecated. It could be implemented later.")
class Chat(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    override fun execute()
    {
        //TODO: acquire or give whole information to another character by chance.
        val who = parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
            .flatMap { it.value.currentCharacters }.filter { it != sbjCharacter }.toHashSet()


        var count = 0


        val factor = .1
        //Mutualities between this character and all other characters in the meeting/conference increase by the number of information exchanged + 1.
        who.forEach {
            parent.setMutuality(sbjCharacter, it, 1.0 + count * factor)
            parent.setMutuality(it, sbjCharacter, 1.0 + count * factor)
        }

        //if (tgtCharacter == who){ println("You chat with yourself.");return}
        parent.characters[sbjCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        return parent.characters[sbjCharacter]!!.currentMeeting != null
    }

}