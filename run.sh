#!/bin/bash

# Run Server Script
# Starts the PaperMC server with optimized JVM settings

echo "[INFO] Starting Colosseum Arena Server..."

if [ ! -f "server/eula.txt" ]; then
    echo "[ERROR] Server not set up. Run './setup.sh [simple|detailed]' first"
    exit 1
fi

./gradlew runServer --no-daemon

# Download Links Used
# https://services.gradle.org/distributions/gradle-8.11-bin.zip
#   | Gradle 8.11 wrapper | gradle/wrapper/gradle-wrapper.properties |
# https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds
#   | PaperMC API - fetch latest build number | build.gradle.kts (downloadPaper task) |
# https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/{BUILD}/downloads/paper-1.21.4-{BUILD}.jar
#   | PaperMC 1.21.4 server JAR | build.gradle.kts (downloadPaper task) |
# https://repo.papermc.io/repository/maven-public/
#   | PaperMC Maven repository | build.gradle.kts (repositories) |
# https://repo1.maven.org/maven2/
#   | Maven Central (Kotlin stdlib) | build.gradle.kts (repositories - via mavenCentral()) |
#
# Project Files Created
# - build.gradle.kts - Gradle build configuration with Kotlin DSL
# - settings.gradle.kts - Project settings
# - src/main/kotlin/com/colosseum/arena/ArenaPlugin.kt - Main plugin
# - src/main/resources/plugin.yml - Plugin metadata
# - setup.sh / setup.bat - Arena type selection scripts
# - run.sh / run.bat - Server startup scripts
# - README.md - Complete documentation
# - .gitignore - Git ignore patterns
# - gradlew / gradlew.bat - Gradle wrapper
# - gradle/wrapper/ - Wrapper configuration
