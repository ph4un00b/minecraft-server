import org.gradle.api.tasks.Exec

tasks.register<Exec>("setupServer") {
    group = "setup"
    description = "Initializes server configuration"
    
    dependsOn("checkJava", "downloadPaper", "downloadPlugins", "jar")
    
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
        
        // Create server.properties from template (if not exists)
        val serverDefaults = file("templates/server.properties.defaults")
        val serverProps = file("server/server.properties")
        if (!serverProps.exists() && serverDefaults.exists()) {
            copy {
                from("templates/server.properties.defaults")
                into("server")
                rename { "server.properties" }
            }
            println("[INFO] Server configuration created from templates/server.properties.defaults")
        } else if (serverProps.exists()) {
            println("[INFO] Server configuration already exists, preserving manual changes")
        } else {
            println("[WARN] templates/server.properties.defaults not found")
        }
        
        // Create phau.properties from template (if not exists)
        val phauDefaults = file("templates/phau.properties.defaults")
        val phauProps = file("server/phau.properties")
        if (!phauProps.exists() && phauDefaults.exists()) {
            copy {
                from("templates/phau.properties.defaults")
                into("server")
                rename { "phau.properties" }
            }
            println("[INFO] Arena configuration created from templates/phau.properties.defaults")
        } else if (phauProps.exists()) {
            println("[INFO] Arena configuration already exists, preserving manual changes")
        } else {
            println("[WARN] templates/phau.properties.defaults not found")
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
