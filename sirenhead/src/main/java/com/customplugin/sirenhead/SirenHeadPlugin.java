package com.yourname.sirenhead;

import org.bukkit.plugin.java.JavaPlugin;

public class SirenHeadPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("SirenHeadPlugin enabled.");
        getCommand("spawnSirenHead").setExecutor(new SirenHeadSpawner(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("SirenHeadPlugin disabled.");
    }
}