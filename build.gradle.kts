import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.citizensnpcs.co/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.citizensnpcs:citizens-main:2.0.37-SNAPSHOT")
    compileOnly("org.mcmonkey:sentinel:2.9.3-SNAPSHOT")
    implementation(kotlin("stdlib"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.isWarnings = true
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
    }
}

// Configure tests to use JUnit Platform
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Read existing version.properties (the source of truth)
val versionPropsFile = file("src/main/resources/version.properties")
val versionProps = Properties()

// Load existing properties
if (versionPropsFile.exists()) {
    versionPropsFile.inputStream().use { versionProps.load(it) }
}

// Get git commit hash (short version)
val gitHash: String by lazy {
    try {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText.getOrElse("unknown").trim()
    } catch (e: Exception) {
        "unknown"
    }
}

// Get build timestamp
val buildTime: String by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
}

// Update version.properties with build info (if git is available, otherwise keep committed values)
// This modifies the file during build, but committed template remains as fallback
if (gitHash != "unknown" && gitHash.isNotEmpty()) {
    versionProps.setProperty("version", "1.0+$gitHash")
    versionProps.setProperty("git.hash", gitHash)
    versionProps.setProperty("build.time", buildTime)

    // Try to get branch name
    val gitBranch: String by lazy {
        try {
            providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
                isIgnoreExitValue = true
            }.standardOutput.asText.getOrElse("unknown").trim()
        } catch (e: Exception) {
            "unknown"
        }
    }
    versionProps.setProperty("git.branch", gitBranch)

    // Write back to file
    versionPropsFile.outputStream().use {
        versionProps.store(it, "Updated by Gradle build - Do not edit manually")
    }

    println("[BUILD] Updated version.properties with git info: 1.0+$gitHash")
} else {
    println(
        "[BUILD] Git not available, " +
            "using committed version.properties values",
    )
}

// Get final version from properties
val fullVersion = versionProps.getProperty("version", "1.0+unknown")
val finalBuildTime = versionProps.getProperty("build.time", buildTime)
val finalGitHash = versionProps.getProperty("git.hash", gitHash)

// Print version info during build
println("[BUILD] Version: $fullVersion")
println("[BUILD] Build Time: $finalBuildTime")
println("[BUILD] Git Hash: $finalGitHash")

tasks.jar {
    archiveFileName.set("colosseum-arena-1.0.jar")
    destinationDirectory.set(file("plugins"))

    // Include runtime dependencies (Kotlin stdlib) in JAR
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        },
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Add build info to manifest
    manifest {
        attributes(
            "Implementation-Version" to fullVersion,
            "Implementation-Title" to "ColosseumArena",
            "Build-Time" to finalBuildTime,
            "Git-Commit" to finalGitHash,
        )
    }
}

// Process plugin.yml - replace @VERSION@ with actual version
tasks.processResources {
    // Replace tokens in plugin.yml
    filesMatching("plugin.yml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to
                mapOf(
                    "VERSION" to fullVersion,
                ),
        )
    }
}

// Apply modular tasks from tasks/ directory
apply(from = "tasks/checkJava.gradle.kts")
apply(from = "tasks/downloadPaper.gradle.kts")
apply(from = "tasks/downloadPlugins.gradle.kts")
apply(from = "tasks/setupServer.gradle.kts")
apply(from = "tasks/runServer.gradle.kts")
apply(from = "tasks/cleanWorld.gradle.kts")

// Full setup task that orchestrates the modular tasks
tasks.register("setup") {
    group = "setup"
    description = "Setup: validate Java, download Paper/Sentinel, " +
        "build plugin, init server"

    dependsOn("setupServer")

    doFirst {
        println("[INFO] Starting server setup...")
        println("[INFO] Plugin Version: $fullVersion")
        println("[INFO] Build Time: $finalBuildTime")
        println("[INFO] Templates: templates/ -> server/")
    }

    doLast {
        println("[INFO] Setup complete!")
        println("[INFO] Plugin built: $fullVersion ($finalBuildTime)")
        println(
            "[INFO] Edit server/phau.properties to configure arena settings",
        )
        println("[INFO] Edit server/server.properties for server settings")
        println("[INFO] Run './start-server.sh' to start")
    }
}
