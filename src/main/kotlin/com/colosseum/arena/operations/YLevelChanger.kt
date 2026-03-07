package com.colosseum.arena.operations

import com.colosseum.core.storage.PropertiesStorage
import com.colosseum.npc.NPCManager
import org.bukkit.World

/**
 * Changes arena Y level and rebuilds
 * Handles the sequence: clear → save new Y → rebuild
 */
class YLevelChanger(
    private val storage: PropertiesStorage,
    private val clearer: ArenaClearer,
    private val npcManager: NPCManager,
) {
    /**
     * Change Y level and rebuild
     * @param world The world to rebuild in
     * @param newY The new Y level
     * @param rebuild Callback to trigger rebuild with current type
     */
    fun change(world: World, newY: Int, rebuild: () -> Unit) {
        // Save new Y level
        storage.setArenaBaseY(newY)

        // Clear the area
        clearer.clear(world)

        // Clear NPCs
        npcManager.clearAllNPCs()

        // Trigger rebuild
        rebuild()
    }
}
