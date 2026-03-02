package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.full.memberFunctions

/**
 *  This is the base class for all game actions. It is used to represent actions that characters can take.
 *  It is also used to represent actions that are taken by the game itself.
 *
 *  Game actions are serialized to JSON and sent to the client. The client then displays the action to the user.
 *  The user then chooses the parameters for the action. The client then sends the action back to the server.
 *
 *  The server then checks the parameters for validity and then executes the action.
 * */
@Serializable
sealed class GameAction() {

    //Anyone thinks this is a stupid design, I agree, but this is due to fundamental design of Kotlinx Serialization library.
    //Read github.com/Kotlin/kotlinx.serialization/issues/599
    abstract val sbjCharacter: String

    /**This can be different from the current place of the subject, in case of a hypothetical action.*/
    abstract val tgtPlace: String

    val sbjCharObj get() = parent.characters[sbjCharacter]!!
    val tgtPlaceObj get() = parent.places[tgtPlace]!!

    val expectedDuration
        get() = //Execution time penalty when the will is low.
            if (parent.getMutuality(sbjCharacter) < ReadOnly.const("CriticalWill")) {
                if (this is NewAgenda || this is Intercept || this is InvestigateAccidentScene || this is ClearAccidentScene || this is PrepareInfo)
                    3 * ReadOnly.constInt(this::class.simpleName!! + "Duration")
                else if (this is Sleep || this is Move)
                    2 * ReadOnly.constInt(this::class.simpleName!! + "Duration")
                else
                    ReadOnly.constInt(this::class.simpleName!! + "Duration")
            } else if (parent.getMutuality(sbjCharacter) < ReadOnly.const("DowntimeWill")) {
                if (this is NewAgenda || this is Intercept || this is InvestigateAccidentScene || this is ClearAccidentScene || this is PrepareInfo)
                    3 * ReadOnly.constInt(this::class.simpleName!! + "Duration") / 2
                else if (this is Sleep || this is Move)
                    ReadOnly.constInt(this::class.simpleName!! + "Duration")
                else
                    ReadOnly.constInt(this::class.simpleName!! + "Duration")

            } else
                ReadOnly.constInt(this::class.simpleName!! + "Duration")

    @Transient
    lateinit var parent: GameState
    fun injectParent(parent: GameState) {
        this.parent = parent
    }

    @Deprecated("In CUI, actions are prepared through player input using this method. Not implemented anymore.")
    open fun chooseParams() {
    }

    /**
     * Checks if the given information is a sufficient proof of this action.
     * Base implementation checks reference. In this case, delegation is impossible.
     * Every override of this method must return true for action information with the only difference in the subject.
     * Otherwise, ExecuteCommandRoutine delegation does not work properly.
     *
     * For some actions such as examine or investigate, resulting information is also sufficient. Other actions such as UnofficialResourceTransfer allows the subject and source to differ from the original action. Hence, these actions override this method.
     */
    open fun isProofOfWork(info: Information): Boolean {
        return (info.type == InformationType.ACTION && info.action == this)

    }

    /**
     * This is used to store why the action is invalid, used by the UI elements to display the reason why the action cannot be performed.
     */
    @Transient
    var invalidReason = ""
    fun reason(predicate: Boolean, reasonKey: String): Boolean {
        if (!predicate) {
            invalidReason = ReadOnly.prop(reasonKey)
        }
        return predicate
    }

    /**
     * This is a test function to check if the action is valid. It is called before execute. You can insert conditions to check here.
     * The execute function is still called even if this function returns false, but the engine throws an exception.
     */
    open fun isValid(): Boolean {
        return true
    }

    //Some gameActions have more complicated freezing mechanism, so they don't call this function.
    open fun execute() {
        sbjCharObj.frozen += expectedDuration
    }

    open fun deltaWill(): MutualityMatrix {
        val deltaMut = MutualityMatrix()
        return deltaMut
    }

    /**Stupid cloning using reflection, assumes all subclasses are data classes.
     * Surely this can't strike me back.
     */
    fun copyRef(newSbj: String): GameAction {
        val copyFun = this::class.memberFunctions.first { it.name == "copy" }
        return copyFun.callBy(mapOf(copyFun.parameters[0] to this, copyFun.parameters[1] to newSbj)) as GameAction
    }

    fun copyRef(newSbj: String, newPlace: String): GameAction {
        val copyFun = this::class.memberFunctions.first { it.name == "copy" }
        return copyFun.callBy(
            mapOf(
                copyFun.parameters[0] to this,
                copyFun.parameters[1] to newSbj,
                copyFun.parameters[2] to newPlace
            )
        ) as GameAction
    }

    fun generateSpeech(): String {
        var text: String
        when (this) {
            is NewAgenda -> {
                when (this.agenda.type) {
                    AgendaType.PROOF_OF_WORK -> text = ReadOnly.script("NewAgenda-ProofOfWork")
                    AgendaType.NOMINATE -> text =
                        ReadOnly.script("NewAgenda-Nominate")
                            .format(ReadOnly.charProp(this.agenda.subjectParams["character"]!!))

                    AgendaType.REQUEST -> text = ReadOnly.script("NewAgenda-Request").format(
                        ReadOnly.prop(
                            this.agenda.attachedRequest!!
                                .action::class.simpleName!!
                        ), this.agenda.attachedRequest!!.issuedTo.first()
                    )

                    AgendaType.PROMISE -> text = ReadOnly.script("NewAgenda-Promise").format(
                        ReadOnly.prop(
                            this.agenda.attachedRequest!!
                                .action::class.simpleName!!
                        )
                    )

                    AgendaType.PRAISE -> text =
                        ReadOnly.script("NewAgenda-Praise")
                            .format(ReadOnly.charProp(this.agenda.subjectParams["character"]!!))

                    AgendaType.DENOUNCE -> text =
                        ReadOnly.script("NewAgenda-Denounce")
                            .format(ReadOnly.charProp(this.agenda.subjectParams["character"]!!))

                    AgendaType.PRAISE_PARTY -> text =
                        ReadOnly.script("NewAgenda-PraiseParty").format(this.agenda.subjectParams["party"])

                    AgendaType.DENOUNCE_PARTY -> text =
                        ReadOnly.script("NewAgenda-DenounceParty").format(this.agenda.subjectParams["party"])

                    AgendaType.BUDGET_PROPOSAL -> text = ReadOnly.script("NewAgenda-BudgetProposal")
                    AgendaType.BUDGET_RESOLUTION -> text = ReadOnly.script("NewAgenda-BudgetResolution")
                    AgendaType.APPOINT_MEETING -> text =
                        ReadOnly.script("NewAgenda-AppointMeeting")

                    AgendaType.FIRE_MANAGER -> text =
                        ReadOnly.script("NewAgenda-FireManager")
                            .format(ReadOnly.charProp(this.agenda.subjectParams["character"]!!))
                }
            }

            is AddInfo -> {
                text = ReadOnly.script(this.effectivityReason)
                    .format(ReadOnly.charProp(this.agenda.subjectParams["character"]!!))
            }

            is EndSpeech -> {
                text = ReadOnly.script("EndSpeech2").format(ReadOnly.charProp(this.nextSpeaker))
            }

            else -> {
                text = ReadOnly.script(this.javaClass.simpleName, this)
            }
        }
        return text
    }

}
