package com.customplugin.sirenhead;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SirenHeadSpawner implements CommandExecutor {
    private final JavaPlugin plugin;

    public SirenHeadSpawner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        SirenHeadMob.spawn(plugin, player.getLocation());
        player.sendMessage("Siren Head has been summoned...");
        return true;
    }
}