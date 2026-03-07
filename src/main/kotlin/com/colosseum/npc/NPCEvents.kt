package com.colosseum.npc

import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent

class NPCEvents : Listener {
    @EventHandler
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Villager) {
            // Handle custom NPC damage logic
            // This will be handled by NPCManager's damage event handler
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity is Villager) {
            // Handle custom NPC death logic
            // This will be handled by NPCManager's death event handler
        }
    }
}
