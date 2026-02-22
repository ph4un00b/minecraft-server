#!/bin/bash

# Setup Script
# Usage: ./scripts/setup.sh (or from project root)
#
# Templates are in templates/ folder:
#   - templates/server.properties.defaults -> server/server.properties
#   - templates/phau.properties.defaults -> server/phau.properties
#
# After setup, edit files in server/ to customize:
#   - server/phau.properties: Arena settings (base Y, type)
#   - server/server.properties: Server settings (port, gamemode, etc)
#
# In-game commands (requires OP):
#   /arena simple      - Build simple arena
#   /arena detailed    - Build detailed arena
#   /arena rebuild     - Rebuild current arena
#   /arena sety <level> - Change arena Y level and rebuild

# Project Name: Colosseum Arena - PaperMC 1.21.4 Minecraft Server with Kotlin Plugin
# Requirements:
# - PaperMC 1.21.4 server with auto-generated arena
# - Kotlin 2.0.x with JVM target 21
# - Zero compiler warnings
# - Clean architecture with separation of concerns
# - Arena base Y level configurable via properties
# - Two arena types: simple and detailed
# - In-game commands:
#   /arena simple      - Build simple arena
#   /arena detailed    - Build detailed arena
#   /arena rebuild     - Rebuild current arena
#   /arena sety <level> - Change arena Y level and rebuild

# Templates are in templates/ folder:
#   - templates/server.properties.defaults -> server/server.properties
#   - templates/phau.properties.defaults -> server/phau.properties
#
# File Structure to Create:
# root/
# ├── .gitignore
# ├── README.md
# ├── build.gradle.kts            # Main build configuration
# ├── settings.gradle.kts         # Project settings
# ├── gradlew / gradlew.bat       # Gradle wrapper (in root)
# ├── scripts/                    # All user scripts
# │   ├── setup.sh / setup.bat    # Setup scripts
# │   ├── run.sh / run.bat        # Server run scripts (Gradle)
# │   ├── start-server.sh / .bat  # Server run scripts (direct Java)
# │   └── fix-locks.sh            # Fix server lock issues
# ├── tasks/
# │   ├── checkJava.gradle.kts
# │   ├── downloadPaper.gradle.kts
# │   ├── setupServer.gradle.kts
# │   ├── runServer.gradle.kts
# │   └── cleanWorld.gradle.kts
# ├── templates/                  # Configuration templates
# │   ├── phau.properties.defaults # Arena config template
# │   └── server.properties.defaults # Server config template
# ├── server/                     # Server files (generated, gitignored)
# │   ├── world/                  # World data
# │   ├── plugins/                # Plugin JAR
# │   ├── phau.properties         # Arena config (from template)
# │   ├── server.properties       # Server config (from template)
# │   └── paper-1.21.4.jar        # Paper server
# ├── external/                   # Downloaded Paper JAR
# ├── plugins/                    # Compiled plugin
# └── src/main/
#     ├── kotlin/com/colosseum/
#     │   ├── arena/
#     │   │   ├── ArenaPlugin.kt
#     │   │   ├── manager/
#     │   │   │   └── ArenaManager.kt
#     │   │   ├── builders/
#     │   │   │   ├── ArenaBuilder.kt
#     │   │   │   ├── SimpleArena.kt
#     │   │   │   └── DetailedArena.kt
#     │   │   ├── operations/
#     │   │   │   ├── ArenaClearer.kt
#     │   │   │   └── YLevelChanger.kt
#     │   │   └── domain/
#     │   │       ├── ArenaType.kt
#     │   │       └── ArenaConfig.kt
#     │   └── core/storage/
#     │       └── PropertiesStorage.kt
#     └── resources/
#         └── plugin.yml
#
#
# CRITICAL: Create All Files Below Exactly
# 1. settings.gradle.kts:
# rootProject.name = "colosseum-arena"
# 2. build.gradle.kts:
# plugins {
#     kotlin("jvm") version "2.0.21"
# }
# repositories {
#     mavenCentral()
#     maven {
#         url = uri("https://repo.papermc.io/repository/maven-public/")
#     }
# }
# dependencies {
#     compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
#     implementation(kotlin("stdlib"))
# }
# kotlin {
#     jvmToolchain(21)
# }
# tasks.withType<JavaCompile> {
#     sourceCompatibility = "21"
#     targetCompatibility = "21"
#     options.isWarnings = true
#     options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
# }
# tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
#     compilerOptions {
#         jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
#         allWarningsAsErrors.set(true)
#     }
# }
# tasks.jar {
#     archiveFileName.set("colosseum-arena-1.0.jar")
#     destinationDirectory.set(file("plugins"))
#     from(configurations.runtimeClasspath.get().map {
#         if (it.isDirectory) it else zipTree(it)
#     })
#     duplicatesStrategy = DuplicatesStrategy.EXCLUDE
# }
# apply(from = "tasks/checkJava.gradle.kts")
# apply(from = "tasks/downloadPaper.gradle.kts")
# apply(from = "tasks/setupServer.gradle.kts")
# apply(from = "tasks/runServer.gradle.kts")
# apply(from = "tasks/cleanWorld.gradle.kts")
# tasks.register("setup") {
#     group = "setup"
#     description = "Complete setup: validate Java, download Paper, build plugin, init server"
#     dependsOn("setupServer")
#     doFirst {
#         println("[INFO] Starting server setup...")
#         println("[INFO] Templates: templates/ -> server/")
#     }
#     doLast {
#         println("[INFO] Setup complete!")
#         println("[INFO] Edit server/phau.properties to configure arena settings")
#         println("[INFO] Edit server/server.properties for server settings")
#         println("[INFO] Run './scripts/start-server.sh' to start")
#     }
# }

# Change to project root if running from scripts directory
if [ -d "../templates" ]; then
    cd ..
fi

echo "[INFO] Setting up Colosseum Arena..."
echo "[INFO] Templates: templates/ -> server/"
echo "[INFO] Edit server/phau.properties for arena settings"

if ! ./gradlew setup --no-daemon; then
    echo "[ERROR] Setup failed"
    exit 1
fi

echo "[INFO] Setup complete!"
echo "[INFO] Edit server/phau.properties to change arena settings"
echo "[INFO] In-game: /arena sety <level> to change Y level"
echo "[INFO] Run './scripts/start-server.sh' to start the server"
