package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class FindCharacterRoutine(val character: String) : Routine() {
    private var waitForCharacter = false
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (place == gState.places.values.find { it.characters.contains(character) }?.name)
            return success()
        //Stop if spent too much time
        if (gState.time - routineStartTime > IDTH) {
            return failed()
        }

        if (!waitForCharacter)
            return MoveRoutine(gState.places.values.find { it.characters.contains(character) }!!.name)
        return null
    }

    override fun onSubroutineFail(subroutine: Routine) {
        //Can't move to the character. For example, they are in their home or other places this character does not have permission to move into.
        //Just wait here
        waitForCharacter = true
    }

    override fun execute(name: String, place: String): GameAction {
        return Wait(name, place)
    }

}
