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

fun downloadFile(urlString: String, destination: java.io.File, maxRetries: Int = 3) {
    val uri = URI(urlString)
    var lastException: Exception? = null
    
    repeat(maxRetries) { attempt ->
        try {
            if (attempt > 0) {
                val delay = attempt * 5L
                println("[INFO] Retrying in ${delay}s...")
                Thread.sleep(delay * 1000)
            }
            
            println("[INFO] Download attempt ${attempt + 1}/$maxRetries...")
            
            val connection = uri.toURL().openConnection().apply {
                setRequestProperty("User-Agent", "Mozilla/5.0 (Gradle)")
                connectTimeout = 10000   // 10 seconds to establish connection
                readTimeout = 30000      // 30 seconds for read operations
            }
            
            // Use buffered streaming with timeout-aware reading
            connection.inputStream.buffered().use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(8192) // 8KB buffer
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastProgressTime = System.currentTimeMillis()
                    
                    while (true) {
                        bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        
                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime > 10000) { // Log every 10 seconds
                            println("[INFO] Downloaded ${formatBytes(totalRead)}...")
                            lastProgressTime = now
                        }
                    }
                    
                    println("[INFO] Download completed: ${formatBytes(totalRead)}")
                }
            }
            
            return  // Success, exit function
            
        } catch (e: Exception) {
            lastException = e
            println("[WARN] Attempt ${attempt + 1}/$maxRetries failed: ${e.message}")
            if (destination.exists() && destination.length() == 0L) {
                destination.delete()  // Clean up empty file
            }
        }
    }
    
    throw lastException ?: RuntimeException("Download failed after $maxRetries attempts")
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes bytes"
    }
}
