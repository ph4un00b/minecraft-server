import java.net.URI
import java.nio.channels.Channels

tasks.register("downloadPlugins") {
    group = "setup"
    description = "Downloads Citizens and Sentinel plugins to server/plugins"
    
    dependsOn("downloadPaper")
    
    doLast {
        file("server/plugins").mkdirs()
        
        // Download Citizens - build 4129 (latest, compatible with Paper 1.21.11+)
        val citizensUrl = "https://ci.citizensnpcs.co/job/Citizens2/4129/artifact/dist/target/Citizens-2.0.41-b4129.jar"
        val citizensFile = file("server/plugins/Citizens.jar")
        
        if (!citizensFile.exists()) {
            println("[INFO] Downloading Citizens build 4129 (latest, 1.21.11+ compatible)...")
            try {
                downloadFile(citizensUrl, citizensFile)
                println("[INFO] Citizens downloaded successfully to server/plugins/Citizens.jar")
            } catch (e: Exception) {
                println("[ERROR] Failed to download Citizens: ${e.message}")
                println("[ERROR] Please manually download from: https://ci.citizensnpcs.co/job/Citizens2/")
                throw e
            }
        } else {
            println("[INFO] Citizens already exists: server/plugins/Citizens.jar")
        }
        
        // Download Sentinel - build 533
        val sentinelUrl = "https://ci.citizensnpcs.co/job/Sentinel/533/artifact/target/Sentinel-2.9.3-SNAPSHOT-b533.jar"
        val sentinelFile = file("server/plugins/Sentinel.jar")
        
        if (!sentinelFile.exists()) {
            println("[INFO] Downloading Sentinel build 533...")
            try {
                downloadFile(sentinelUrl, sentinelFile)
                println("[INFO] Sentinel downloaded successfully to server/plugins/Sentinel.jar")
            } catch (e: Exception) {
                println("[ERROR] Failed to download Sentinel: ${e.message}")
                println("[ERROR] Please manually download from: https://ci.citizensnpcs.co/job/Sentinel/")
                throw e
            }
        } else {
            println("[INFO] Sentinel already exists: server/plugins/Sentinel.jar")
        }
        
        println("[INFO] All required plugins downloaded")
    }
}

fun downloadFile(urlString: String, destination: java.io.File) {
    val uri = URI(urlString)
    val connection = uri.toURL().openConnection()
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Gradle)")
    
    connection.inputStream.use { inputStream ->
        Channels.newChannel(inputStream).use { readableByteChannel ->
            destination.outputStream().use { outputStream ->
                outputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
            }
        }
    }
}
