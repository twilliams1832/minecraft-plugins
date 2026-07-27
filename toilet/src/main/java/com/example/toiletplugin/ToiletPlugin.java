package com.example.toiletplugin;

import com.example.toiletplugin.listeners.PlayerQuitListener;
import com.example.toiletplugin.listeners.ToiletInteractListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ToiletPlugin - Main plugin entry point.
 * Registers all listeners and commands, and manages the ToiletManager lifecycle.
 */
public class ToiletPlugin extends JavaPlugin {

    private ToiletManager toiletManager;

    @Override
    public void onEnable() {
        // Initialize the central manager that tracks all toilet instances
        this.toiletManager = new ToiletManager(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ToiletInteractListener(this, toiletManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(toiletManager), this);

        // Register the /placetoilet command handler
        getCommand("placetoilet").setExecutor(this);

        getLogger().info("ToiletPlugin enabled! Nature calls.");
    }

    @Override
    public void onDisable() {
        // Clean up all ArmorStands and dismount players when plugin is disabled
        if (toiletManager != null) {
            toiletManager.removeAllToilets();
        }
        getLogger().info("ToiletPlugin disabled. The toilet has been flushed.");
    }

    /**
     * Handles the /placetoilet command.
     * Gives the player a custom toilet item they can place later.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("placetoilet")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can receive toilet items.");
                return true;
            }
            if (!player.hasPermission("toiletplugin.place")) {
                player.sendMessage("§cYou don't have permission to place toilets.");
                return true;
            }
            toiletManager.giveToiletItem(player);
            return true;
        }
        return false;
    }

    public ToiletManager getToiletManager() {
        return toiletManager;
    }
}
