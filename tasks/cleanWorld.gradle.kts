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

tasks.register<Delete>("cleanPlugins") {
    group = "maintenance"
    description = "Deletes downloaded Citizens and Sentinel plugins"
    
    delete("server/plugins/Citizens.jar")
    delete("server/plugins/Sentinel.jar")
    
    doLast {
        println("[INFO] Citizens and Sentinel plugin JARs deleted.")
        println("[INFO] Run './gradlew setup' or './gradlew downloadPlugins' to re-download.")
    }
}

tasks.register("cleanAll") {
    group = "maintenance"
    description = "Deletes world folders AND plugin JARs (complete reset)"
    
    dependsOn("cleanWorld", "cleanPlugins")
    
    doLast {
        println("[INFO] Complete cleanup finished.")
        println("[INFO] Run './gradlew setup' to re-download everything and start fresh.")
    }
}
