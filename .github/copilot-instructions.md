# TitaniumPolitics - LibGDX Political Simulation Game

TitaniumPolitics is a political simulation game built with LibGDX game engine in Kotlin. The project simulates complex political scenarios in a space station environment with multiple characters, factions, and resource management systems.

**ALWAYS** reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites
- **Java 17**: The project requires Java 17. Current environment has OpenJDK 17.0.16.
- **Gradle 7.5.1**: Uses Gradle wrapper for build management.
- **Network Access**: Build requires external dependencies from jitpack.io and dl.google.com.

### Bootstrap and Build
**CRITICAL BUILD LIMITATIONS**: The build currently fails due to network connectivity issues with external dependencies:
- `com.github.raeleus.TenPatch:tenpatch:5.2.3` from jitpack.io
- `space.earlygrey:shapedrawer:2.5.0` from jitpack.io

Commands to try (with known limitations):
```bash
# Make gradlew executable (required first time)
chmod +x gradlew

# Check Gradle version - WORKS
./gradlew --version

# Clean project - WORKS (takes ~1 second)
./gradlew clean

# List all tasks - WORKS (takes ~20 seconds)
./gradlew tasks --all

# Build project - FAILS due to network dependencies
./gradlew build
# EXPECTED ERROR: Could not resolve external dependencies from jitpack.io

# Compile Kotlin - FAILS due to same network dependencies  
./gradlew compileKotlin
# EXPECTED ERROR: Could not resolve com.github.raeleus.TenPatch:tenpatch:5.2.3
```

**NEVER CANCEL**: Build attempts may take several minutes to fail due to network timeouts. Always wait at least 5 minutes before considering alternatives.

### Development Environment Setup
```bash
# Verify Java version
java -version
# Expected: OpenJDK 17.x.x

# Check project structure
ls -la
# Expected: build.gradle, settings.gradle, core/, desktop/, assets/, gradlew

# Verify Gradle wrapper permissions
ls -la gradlew
# Should be executable (-rwxr-xr-x)

# If gradlew is not executable, make it executable:
chmod +x gradlew
```

## Project Structure

### Core Architecture
- **Two-module Gradle project**:
  - `core/`: Main game logic, UI, and game engine integration (143+ Kotlin files)
  - `desktop/`: Desktop application launcher using LWJGL3 backend
- **Main entry point**: `desktop/src/com/titaniumPolitics/game/DesktopLauncher.kt`
- **Game entry class**: `core/src/com/titaniumPolitics/game/EntryClass.kt`

### Key Directories
```
├── core/src/com/titaniumPolitics/game/
│   ├── EntryClass.kt              # Main game application
│   ├── core/                      # Game logic and state management
│   │   ├── GameEngine.kt          # Core game engine
│   │   ├── GameState.kt           # Game state management
│   │   ├── ReadOnly.kt            # Configuration and constants
│   │   ├── gameActions/           # Player and NPC actions
│   │   └── NPCRoutines/           # AI behavior systems
│   ├── events/                    # Game events and story elements
│   └── ui/                        # User interface components
├── desktop/src/com/titaniumPolitics/game/
│   └── DesktopLauncher.kt         # Desktop application entry
└── assets/                        # Game assets and data
    ├── json/                      # Game configuration files
    ├── data/                      # Images, sounds, and media
    ├── Fonts/                     # Typography assets
    └── skin/                      # UI theme definitions
```

### Critical Configuration Files
- `assets/json/init.json`: Initial game state with characters, places, and parties
- `assets/json/consts.json`: Game timing and balance constants  
- `assets/json/map.json`: Location and world structure definitions
- `assets/json/characters.json`: Character definitions and traits
- `assets/json/action.json`: Available player actions

## Running the Application

### Desktop Application
```bash
# Run the desktop version (if build succeeds)
./gradlew desktop:run
# CURRENT STATUS: Cannot run due to build dependencies

# Check what would run (dry run)
./gradlew desktop:run --dry-run
# This works and shows the task dependency chain
```

**Expected Window**: 1500x800 pixels titled "titaniumPolitics" running at 60 FPS.

## Validation and Testing

### No Test Suite
**IMPORTANT**: This project currently has no automated test suite. Manual validation is required for all changes.

### Manual Validation Steps
When making changes to the game logic:

1. **Verify JSON Configuration**: 
   ```bash
   # Check JSON syntax
   python3 -m json.tool assets/json/init.json > /dev/null
   python3 -m json.tool assets/json/consts.json > /dev/null
   python3 -m json.tool assets/json/map.json > /dev/null
   ```

2. **Validate Kotlin Syntax**:
   ```bash
   # Attempt compilation (will fail at dependency resolution)
   ./gradlew compileKotlin
   # Look for Kotlin compilation errors before dependency errors
   ```

3. **Check Asset References**:
   - Verify image files referenced in JSON exist in `assets/data/`
   - Confirm sound files are present for background music
   - Validate font files in `assets/Fonts/`

### Expected Game Scenarios
If the application could run, validate these core workflows:
- **Game Startup**: Main menu displays with "Click to Start" button
- **Game Initialization**: Loads character positions and political factions
- **Character Interaction**: Political simulation with resource management
- **Save/Load**: Game state serialization to JSON files

## Common Development Tasks

### Adding New Characters
1. Edit `assets/json/init.json` to add character definition
2. Update `assets/json/characters.json` for character properties
3. Add character image to `assets/data/` directory
4. Update faction assignments in parties section

### Modifying Game Balance
1. Edit `assets/json/consts.json` for timing and thresholds
2. Values are referenced via `ReadOnly.const("parameterName")` in Kotlin code
3. Changes take effect immediately on game restart

### UI Modifications
1. UI components are in `core/src/com/titaniumPolitics/game/ui/`
2. Skin definitions in `assets/skin/titaniumSkin.json`
3. Font loading handled in `EntryClass.kt` using FreeType

### Audio and Visual Assets
- **Background music**: `assets/data/mainMenu.mp3`, `assets/data/Capsule_old_lighthouse_loop.mp3`
- **Sound effects**: Various `.mp3` files in `assets/data/` for game actions
- **Images**: Location backgrounds and character portraits in `assets/data/`

## Development Limitations

### Network Dependencies
**CRITICAL**: The following dependencies cannot be resolved in restricted network environments:
- `com.github.raeleus.TenPatch:tenpatch:5.2.3`
- `space.earlygrey:shapedrawer:2.5.0`

### Workarounds
- Use `./gradlew clean` to verify Gradle setup
- Use `./gradlew tasks` to explore available operations  
- Edit source files directly and validate syntax manually
- Test JSON configuration files with external validators

### Build Time Expectations
- **Clean**: ~1 second (verified: 0.97s)
- **Task listing**: ~1 second (verified: 1.03s)
- **Build attempt**: Fails after ~3-5 seconds due to network issues
- **Dry run tasks**: ~1 second (verified: 0.62s)
- **NEVER CANCEL**: Always wait for network timeouts to complete

## Key Classes and Systems

### Game Engine (`core/GameEngine.kt`)
- **Purpose**: Main game loop and turn progression
- **Key Methods**: `progression()`, `performAction()`, `startGame()`
- **Timing**: Game operates in discrete time units with day/night cycles

### Game State (`core/GameState.kt`) 
- **Purpose**: Centralized state management and serialization
- **Features**: Character relationships, resource tracking, save/load functionality
- **Serialization**: Uses Kotlinx Serialization for JSON persistence

### Character AI (`core/NPCRoutines/`)
- **ExecuteCommandRoutine**: NPC action execution system
- **MoveRoutine**: Character movement between locations
- **Behavioral**: AI-driven character decision making

### User Interface (`ui/`)
- **MainMenu**: Game startup screen
- **CapsuleStage**: Main game interface
- **CharacterPortraitsUI**: Character interaction system
- **MeetingUI**: Political meeting interface

## Asset Management

### Audio System
- Uses LibGDX audio for background music and sound effects
- Music loops automatically during gameplay
- Sound effects triggered by player actions and events

### Graphics Pipeline  
- LibGDX texture loading with asset manager
- Dynamic background switching based on player location
- Character portraits and location images

### Localization
- Text properties in `assets/texts/`
- UI strings externalized for potential translation
- Character dialogue and story text in separate files

Remember: Always validate changes manually since there are no automated tests. Focus on JSON configuration integrity and Kotlin syntax correctness when making modifications.