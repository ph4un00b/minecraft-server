package com.colosseum.arena

import org.bukkit.entity.Villager
import org.bukkit.event.entity.EntityDamageEvent

class NPCConfig(
    var health: Double = 1.0,
    var count: Int = 4,
    var enabled: Boolean = true
)