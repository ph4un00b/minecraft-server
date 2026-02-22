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

tasks.jar {
    archiveFileName.set("colosseum-arena-1.0.jar")
    destinationDirectory.set(file("plugins"))

    // Include runtime dependencies (Kotlin stdlib) in JAR
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Apply modular tasks from tasks/ directory
apply(from = "tasks/checkJava.gradle.kts")
apply(from = "tasks/downloadPaper.gradle.kts")
apply(from = "tasks/setupServer.gradle.kts")
apply(from = "tasks/runServer.gradle.kts")
apply(from = "tasks/cleanWorld.gradle.kts")

// Arena type configuration via Gradle property
// Usage: ./gradlew setup -ParenaType=simple (or detailed)
// Default: detailed
val arenaType: String by extra {
    (project.findProperty("arenaType") as? String)?.lowercase() 
        ?: System.getenv("ARENA_TYPE")?.lowercase()
        ?: "detailed"
}

// Validate arena type
if (arenaType !in listOf("simple", "detailed")) {
    throw GradleException("[ERROR] Invalid arena type '$arenaType'. Use 'simple' or 'detailed'")
}

// Full setup task that orchestrates the modular tasks
tasks.register("setup") {
    group = "setup"
    description = "Complete setup: validate Java, download Paper, build plugin, init server. Use -ParenaType=simple or detailed"

    dependsOn("setupServer")

    doFirst {
        println("[INFO] Setting up with arena type: $arenaType")
        // Pass arena type to the running server via system property
        System.setProperty("arena.type", arenaType)
    }

    doLast {
        println("[INFO] Setup complete with $arenaType arena!")
        println("[INFO] Run './gradlew runServer' or './start-server.sh' to start")
    }
}
