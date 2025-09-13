package com.customplugin.bulldozer;

import org.bukkit.plugin.java.JavaPlugin;

public class BulldozerPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("Bulldozer plugin enabled!");
        this.getCommand("bulldozer").setExecutor(new BulldozerCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("Bulldozer plugin disabled.");
    }
}
