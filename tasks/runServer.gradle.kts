import org.gradle.api.tasks.Exec

tasks.register<Exec>("runServer") {
    group = "run"
    description = "Starts the PaperMC server with interactive console"
    
    workingDir("server")
    
    doFirst {
        if (!file("server/eula.txt").exists()) {
            throw GradleException("[ERROR] Server not set up. Run './gradlew setupServer' first.")
        }
        
        println("[INFO] Starting PaperMC server...")
        println("[INFO] Type 'help' for commands, 'stop' to shutdown")
    }
    
    // JVM Flags - hardcoded for consistent performance
    // -Xms511M -Xmx511M           : Fixed 511MB heap memory
    // -XX:+UseG1GC                : G1 Garbage Collector for low latency
    // -XX:+ParallelRefProcEnabled  : Parallel reference processing
    // -XX:MaxGCPauseMillis=200     : Target 200ms GC pauses
    // -XX:+UnlockExperimentalVMOptions : Enable experimental flags
    // -XX:+DisableExplicitGC       : Ignore System.gc() calls
    // -XX:G1NewSizePercent=30     : Young generation min 30%
    // -XX:G1MaxNewSizePercent=40    : Young generation max 40%
    // -XX:G1HeapRegionSize=8M      : 8MB heap regions
    // -XX:G1ReservePercent=20       : 20% heap reserve
    // -XX:G1HeapWastePercent=5       : Reclaim <5% live data regions
    // -Dpaper.disableWatchdog=true   : Disable watchdog (single-threaded)
    // -Djava.awt.headless=true      : No GUI mode
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
