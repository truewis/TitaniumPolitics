import java.util.Properties
import java.io.FileInputStream

fun main() {
    val script = Properties()
    script.load(FileInputStream("assets/texts/DefaultCharacter.properties"))
    
    val actionClasses = listOf(
        "AddInfo", "AppointMeeting", "BudgetProposal", "BudgetResolution", "Chat", 
        "ClearAccidentScene", "Command", "Eat", "EndMeeting", "EndSpeech", "Examine", 
        "InfoAnnounce", "InfoRequest", "InfoShare", "Intercept", "InvestigateAccidentScene", 
        "JoinMeeting", "LeaderAssignment", "LeaveMeeting", "Move", "NewAgenda", 
        "OfficialResourceTransfer", "PrepareInfo", "Repair", "Resign", "Salary", 
        "Sleep", "StartMeeting", "Talk", "Trade", "UnofficialResourceTransfer", 
        "UseItem", "Wait", "criticize", "home", "observeRequest", "praise", "save", 
        "unofficialCommand"
    )
    
    println("Checking for missing properties:")
    for (actionClass in actionClasses) {
        if (script.getProperty(actionClass) == null) {
            println("Missing: $actionClass")
        } else {
            println("Found: $actionClass = ${script.getProperty(actionClass)}")
        }
    }
}
