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

// Full setup task that orchestrates the modular tasks
// Arena configuration is in phau.properties (single source of truth)
tasks.register("setup") {
    group = "setup"
    description = "Complete setup: validate Java, download Paper, build plugin, init server"

    dependsOn("setupServer")

    doFirst {
        println("[INFO] Starting server setup...")
        println("[INFO] Template: phau.properties.defaults -> server/phau.properties")
    }

    doLast {
        println("[INFO] Setup complete!")
        println("[INFO] Edit server/phau.properties to configure arena settings")
        println("[INFO] Run './start-server.sh' to start")
    }
}
