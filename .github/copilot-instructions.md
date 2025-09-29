# Titanium Politics
Titanium Politics is a LibGDX-based political strategy game written in Kotlin, where players navigate political intrigue on a space station. The game features complex character interactions, resource management, and political meeting mechanics.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively
- Bootstrap and build the repository:
  - Ensure Java 17+ is available: `java -version`
  - `chmod +x gradlew` (if needed)  
  - `./gradlew build` -- takes 15-45 seconds to complete. NEVER CANCEL. Set timeout to 90+ seconds.
- Run tests:
  - `./gradlew test` -- takes 1-2 seconds (no tests currently exist in project)
- Verify compilation:
  - `./gradlew classes` -- compiles all classes without full packaging (about 20 seconds)
  - `./gradlew desktop:classes` -- compiles just the desktop module (faster incremental)
- Clean build:
  - `./gradlew clean build` -- for full clean build (about 17 seconds from scratch)

## Key Project Structure
- **core/**: Main game logic (75+ Kotlin files)
  - `core/src/com/titaniumPolitics/game/core/`: Core game systems
    - `GameEngine.kt` - Main game loop and mechanics
    - `GameState.kt` - Game state management and serialization  
    - `gameActions/` - All possible player/NPC actions (Move, Wait, etc.)
    - `NPCRoutines/` - AI behavior patterns for non-player characters
  - `core/src/com/titaniumPolitics/game/ui/` - User interface components
  - `EntryClass.kt` - Application entry point and LibGDX setup
- **desktop/**: Desktop launcher using LWJGL3
  - `DesktopLauncher.kt` - Main class for desktop application
- **assets/**: Game resources
  - `json/` - Game configuration (characters, constants, maps)
  - `texts/` - Dialogue and story content  
  - `data/` - Images, sounds, and other media

## Build System Details
- Uses Gradle 7.5.1 with Kotlin 1.9.0
- Multi-module project: root project configures core + desktop modules
- LibGDX 1.12.0 for graphics and game framework
- Kotlin multiplatform setup with JVM target
- No linting tools configured (only Kotlin compiler warnings)

## Development Workflow
- **NEVER CANCEL ANY BUILD COMMAND** - Build takes 15-45 seconds, set timeout to 90+ seconds minimum
- Always run `./gradlew build` after making changes to verify compilation
- The desktop application requires a graphics environment and cannot run in headless mode
- Build warnings are normal (unchecked casts, unused variables) but should not increase
- No tests exist currently - validation is primarily through successful compilation

## Validation
- Always run `./gradlew build` before committing changes
- Check for new compilation errors or warnings
- The application cannot be functionally tested in headless environments due to LibGDX/LWJGL graphics requirements
- Test JSON configuration changes by ensuring the game still compiles after asset modifications

## Common Tasks
The following are outputs from frequently run commands. Reference them instead of viewing, searching, or running bash commands to save time.

### Root directory structure
```
.gitignore
assets/          # Game resources (JSON configs, images, sounds, text)
build.gradle     # Main build configuration  
core/            # Core game logic in Kotlin
desktop/         # Desktop launcher module
gradle/          # Gradle wrapper files
gradle.properties
gradlew          # Gradle wrapper script (make executable with chmod +x)
gradlew.bat
settings.gradle  # Module configuration
```

### Available Gradle tasks (most important)
```
build - Assembles and tests this project (15-45 seconds, NEVER CANCEL)
classes - Assembles main classes (faster than full build)
clean - Deletes the build directory
test - Runs tests (currently none exist)
desktop:run - Attempts to run desktop app (fails in headless environment)
desktop:dist - Creates distributable JAR
```

### Core game components you'll frequently work with
- **GameAction classes** (core/src/.../gameActions/): Define what players/NPCs can do
- **GameEngine.kt**: Main game loop, handles character actions and progression  
- **GameState.kt**: Manages all game data, save/load functionality
- **Character.kt**: Character properties and behaviors
- **Place.kt**: Location definitions and properties
- **UI components** (core/src/.../ui/): Game interface and menus

### Making changes
- Gameplay mechanics: Modify GameAction classes or add new ones
- Character behavior: Update NPCRoutines classes
- Game balance: Edit assets/json/consts.json for game parameters
- UI changes: Modify files in core/src/.../ui/ directory
- New content: Add to assets/texts/ for dialogue, assets/json/ for configuration

### Common warnings (normal, don't add new ones)
The build shows Kotlin compiler warnings for unchecked casts and unused variables. These are existing technical debt - don't add new warnings with your changes.