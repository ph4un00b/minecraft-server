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
