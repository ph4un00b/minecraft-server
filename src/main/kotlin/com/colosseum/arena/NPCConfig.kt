package com.colosseum.arena

data class NPCConfig(
    var health: Double = 20.0,
    var damage: Double = 5.0,
    var count: Int = 1,
    var enabled: Boolean = true
)
