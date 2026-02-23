import java.text.SimpleDateFormat
import java.util.Date

plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))
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

// Full version string: 1.0+abc1234 (SemVer style)
val fullVersion = "1.0+$gitHash"

// Print version info during build
println("[BUILD] Version: $fullVersion")
println("[BUILD] Time: $buildTime")
println("[BUILD] Git Hash: $gitHash")

tasks.jar {
    archiveFileName.set("colosseum-arena-1.0.jar")
    destinationDirectory.set(file("plugins"))

    // Include runtime dependencies (Kotlin stdlib) in JAR
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Add build info to manifest
    manifest {
        attributes(
            "Implementation-Version" to fullVersion,
            "Implementation-Title" to "ColosseumArena",
            "Build-Time" to buildTime,
            "Git-Commit" to gitHash
        )
    }
}

// Process plugin.yml to inject version
tasks.processResources {
    // Replace tokens in plugin.yml
    filesMatching("plugin.yml") {
        filter<org.apache.tools.ant.filters.ReplaceTokens>(
            "tokens" to mapOf(
                "VERSION" to fullVersion,
                "BUILD_TIME" to buildTime,
                "GIT_HASH" to gitHash
            )
        )
    }
}

// Apply modular tasks from tasks/ directory
apply(from = "tasks/checkJava.gradle.kts")
apply(from = "tasks/downloadPaper.gradle.kts")
apply(from = "tasks/setupServer.gradle.kts")
apply(from = "tasks/runServer.gradle.kts")
apply(from = "tasks/cleanWorld.gradle.kts")

// Full setup task that orchestrates the modular tasks
// Arena configuration is in phau.properties (single source of truth)
tasks.register("setup") {
    group = "setup"
    description = "Complete setup: validate Java, download Paper, build plugin, init server"

    dependsOn("setupServer")

    doFirst {
        println("[INFO] Starting server setup...")
        println("[INFO] Plugin Version: $fullVersion")
        println("[INFO] Build Time: $buildTime")
        println("[INFO] Templates: templates/ -> server/")
    }

    doLast {
        println("[INFO] Setup complete!")
        println("[INFO] Plugin built: $fullVersion ($buildTime)")
        println("[INFO] Edit server/phau.properties to configure arena settings")
        println("[INFO] Edit server/server.properties for server settings")
        println("[INFO] Run './start-server.sh' to start")
    }
}
