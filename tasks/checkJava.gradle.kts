import org.gradle.api.tasks.Exec

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
