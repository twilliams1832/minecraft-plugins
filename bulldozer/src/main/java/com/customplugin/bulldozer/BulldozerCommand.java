package com.customplugin.bulldozer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BulldozerCommand implements CommandExecutor {

    private final BulldozerPlugin plugin;

    public BulldozerCommand(BulldozerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Location loc = player.getLocation();
        Minecart bulldozer = loc.getWorld().spawn(loc, Minecart.class, cart -> {
            cart.setCustomName("Bulldozer");
            cart.setCustomNameVisible(true);
            cart.setInvulnerable(true);
            cart.setMaxSpeed(0.6);
            cart.setDerail(false);
            cart.setSlowWhenEmpty(true);
        });

        // Mount player
        bulldozer.addPassenger(player);

        // Attach clearing logic
        new BulldozerEntity(plugin, bulldozer).start();

        player.sendMessage("Bulldozer spawned!");
        return true;
    }
}
