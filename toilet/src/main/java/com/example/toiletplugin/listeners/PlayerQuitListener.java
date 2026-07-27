package com.example.toiletplugin.listeners;

import com.example.toiletplugin.ToiletManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player disconnect cleanup.
 *
 * If a player quits while seated on a toilet, we must:
 *  1. Remove them from the ArmorStand passenger list.
 *  2. Free the toilet instance so others can use it.
 *
 * Failing to do this would leave the toilet permanently occupied until server restart.
 */
public class PlayerQuitListener implements Listener {

    private final ToiletManager manager;

    public PlayerQuitListener(ToiletManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Gracefully dismount the player if they were seated
        manager.dismountPlayer(event.getPlayer());
    }
}
