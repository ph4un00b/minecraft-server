import org.gradle.api.tasks.Exec
import java.net.URI

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
    compileOnly(kotlin("stdlib"))
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
}

// Java version check
tasks.register<Exec>("checkJava") {
    group = "verification"
    description = "Validates Java 21 is installed"
    
    commandLine("java", "-version")
    
    doFirst {
        val javaVersion = System.getProperty("java.version")
        if (!javaVersion.startsWith("21")) {
            throw GradleException("[ERROR] Java 21 is required. Detected: $javaVersion. Please install JDK 21.")
        }
        println("[INFO] Java version validated: $javaVersion")
    }
}

// Download PaperMC
tasks.register("downloadPaper") {
    group = "setup"
    description = "Downloads latest Paper 1.21.4 build"
    
    val paperDir = file("external")
    val paperJar = file("external/paper-1.21.4.jar")
    
    onlyIf { !paperJar.exists() }
    
    doLast {
        paperDir.mkdirs()
        
        println("[INFO] Fetching PaperMC build info...")
        val apiUrl = URI("https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds").toURL()
        val jsonText = apiUrl.readText()
        
        // Parse JSON to find highest build number
        val buildRegex = """"build":(\d+)""".toRegex()
        val builds = buildRegex.findAll(jsonText).map { it.groupValues[1].toInt() }.toList()
        
        if (builds.isEmpty()) {
            throw GradleException("[ERROR] Could not parse PaperMC builds from API response")
        }
        
        val highestBuild = builds.maxOrNull()!!
        println("[INFO] Latest Paper 1.21.4 build: $highestBuild")
        
        val downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/$highestBuild/downloads/paper-1.21.4-$highestBuild.jar"
        println("[INFO] Downloading from: $downloadUrl")
        
        URI(downloadUrl).toURL().openStream().use { input ->
            paperJar.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        println("[INFO] Paper downloaded successfully to external/paper-1.21.4.jar")
        
        // Store the build number for reference
        file("external/.build-number").writeText(highestBuild.toString())
    }
}

// Setup server
tasks.register<Exec>("setupServer") {
    group = "setup"
    description = "Initializes server configuration"
    
    dependsOn("checkJava", "downloadPaper", "jar")
    
    workingDir("server")
    
    doFirst {
        file("server").mkdirs()
        
        // Copy plugin to server plugins
        file("server/plugins").mkdirs()
        copy {
            from("plugins/colosseum-arena-1.0.jar")
            into("server/plugins")
        }
        
        // Copy Paper to server
        copy {
            from("external/paper-1.21.4.jar")
            into("server")
        }
        
        // Create server.properties
        file("server/server.properties").writeText("""
            |# Colosseum Arena Server Properties
            |server-port=25565
            |gamemode=creative
            |difficulty=peaceful
            |level-type=flat
            |max-players=20
            |spawn-protection=0
            |view-distance=12
            |simulation-distance=10
            |motd=\\u00A72Colosseum Arena\\u00A7r - Gothic Battleground
            |enable-command-block=false
            |generate-structures=false
            |spawn-npcs=false
            |spawn-animals=false
            |spawn-monsters=false
        """.trimMargin())
        
        println("[INFO] Server configuration created")
    }
    
    commandLine("java", "-jar", "paper-1.21.4.jar", "--initSettings")
    
    doLast {
        // Accept EULA
        file("server/eula.txt").writeText("eula=true\n")
        println("[WARN] Minecraft EULA auto-accepted. By continuing you agree to https://aka.ms/MinecraftEULA")
    }
}

// Run server
tasks.register<Exec>("runServer") {
    group = "run"
    description = "Starts the PaperMC server"
    
    workingDir("server")
    
    doFirst {
        if (!file("server/eula.txt").exists()) {
            throw GradleException("[ERROR] Server not set up. Run './gradlew setupServer' first.")
        }
        
        println("[INFO] Starting PaperMC server...")
    }
    
    commandLine(
        "java",
        "-Xms511M", "-Xmx511M",
        "-XX:+UseG1GC",
        "-XX:+ParallelRefProcEnabled",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+DisableExplicitGC",
        "-XX:G1NewSizePercent=30",
        "-XX:G1MaxNewSizePercent=40",
        "-XX:G1HeapRegionSize=8M",
        "-XX:G1ReservePercent=20",
        "-XX:G1HeapWastePercent=5",
        "-Dpaper.disableWatchdog=true",
        "-Djava.awt.headless=true",
        "-jar", "paper-1.21.4.jar",
        "--nogui"
    )
}

// Clean world (for arena rebuild)
tasks.register<Delete>("cleanWorld") {
    group = "maintenance"
    description = "Deletes world folder to regenerate arena"
    
    delete("server/world")
    delete("server/world_nether")
    delete("server/world_the_end")
    
    doLast {
        println("[INFO] World folders deleted. Arena will regenerate on next server start.")
    }
}

// Full setup task
tasks.register("setup") {
    group = "setup"
    description = "Complete setup: validate Java, download Paper, build plugin, init server"
    
    dependsOn("setupServer")
    
    doLast {
        println("[INFO] Setup complete! Run './gradlew runServer' to start the server.")
    }
}
