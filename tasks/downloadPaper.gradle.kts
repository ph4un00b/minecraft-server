import java.net.URI

tasks.register("downloadPaper") {
    group = "setup"
    description = "Downloads latest Paper 1.21.11 build (includes Happy Ghast, compatible with Citizens)"
    
    dependsOn("checkJava")
    
    val paperDir = file("external")
    val paperJar = file("external/paper-1.21.11.jar")
    
    onlyIf { !paperJar.exists() }
    
    doLast {
        paperDir.mkdirs()
        
        println("[INFO] Fetching PaperMC build info...")
        val apiUrl = URI("https://api.papermc.io/v2/projects/paper/versions/1.21.11/builds").toURL()
        val jsonText = apiUrl.readText()
        
        // Parse JSON to find highest build number
        val buildRegex = """"build":(\d+)""".toRegex()
        val builds = buildRegex.findAll(jsonText).map { it.groupValues[1].toInt() }.toList()
        
        if (builds.isEmpty()) {
            throw GradleException("[ERROR] Could not parse PaperMC builds from API response")
        }
        
        val highestBuild = builds.maxOrNull()!!
        println("[INFO] Latest Paper 1.21.11 build: $highestBuild")
        
        val downloadUrl = "https://api.papermc.io/v2/projects/paper/versions/1.21.11/builds/$highestBuild/downloads/paper-1.21.11-$highestBuild.jar"
        println("[INFO] Downloading from: $downloadUrl")
        
        URI(downloadUrl).toURL().openStream().use { input ->
            paperJar.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        println("[INFO] Paper downloaded successfully to external/paper-1.21.11.jar")
        println("[INFO] Happy Ghast entity supported!")
        
        // Store the build number for reference
        file("external/.build-number").writeText(highestBuild.toString())
    }
}
