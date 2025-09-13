package com.customplugin.bulldozer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;

public class BulldozerEntity {

    private final BulldozerPlugin plugin;
    private final Minecart bulldozer;

    public BulldozerEntity(BulldozerPlugin plugin, Minecart bulldozer) {
        this.plugin = plugin;
        this.bulldozer = bulldozer;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!bulldozer.isValid() || bulldozer.getPassengers().isEmpty()) return;

            var loc = bulldozer.getLocation().add(bulldozer.getLocation().getDirection().multiply(1.5));
            Block block = loc.getBlock();

            if (isClearable(block.getType())) {
                block.setType(Material.AIR);
            }
        }, 0L, 5L); // every 5 ticks (~0.25s)
    }

    protected boolean isClearable(Material material) {
        return switch (material) {
            case GRASS_BLOCK, DIRT, SNOW, SAND -> true;
            default -> false;
        };
    }
}
