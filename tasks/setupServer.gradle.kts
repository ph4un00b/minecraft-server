import org.gradle.api.tasks.Exec

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
        
        // Create server.properties only if it doesn't exist (preserve manual edits)
        val propsFile = file("server/server.properties")
        if (!propsFile.exists()) {
            propsFile.writeText("""
                |# Colosseum Arena Server Properties
                |server-port=25565
                |gamemode=creative
                |difficulty=peaceful
                |level-type=flat
                |max-players=4
                |spawn-protection=0
                |view-distance=12
                |simulation-distance=10
                |motd=Gothic Battleground
                |enable-command-block=false
                |generate-structures=false
                |spawn-npcs=false
                |spawn-animals=false
                |spawn-monsters=false
                |online-mode=false
                |enforce-secure-profile=false
            """.trimMargin())
            println("[INFO] Server configuration created")
        } else {
            println("[INFO] Server configuration already exists, preserving manual changes")
        }
        
        println("[INFO] Server files prepared")
    }
    
    commandLine("java", "-jar", "paper-1.21.4.jar", "--initSettings")
    
    doLast {
        // Accept EULA
        file("server/eula.txt").writeText("eula=true\n")
        println("[WARN] Minecraft EULA auto-accepted. By continuing you agree to https://aka.ms/MinecraftEULA")
    }
}
