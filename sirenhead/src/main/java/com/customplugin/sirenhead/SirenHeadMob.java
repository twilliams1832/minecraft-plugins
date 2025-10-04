package com.customplugin.sirenhead;

import org.bukkit.Location;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.java.JavaPlugin;

public class SirenHeadMob {
    public static Zombie spawn(JavaPlugin plugin, Location location) {
        Zombie sirenHead = location.getWorld().spawn(location, Zombie.class);
        sirenHead.setCustomName("§cSiren Head");
        sirenHead.setCustomNameVisible(true);
        sirenHead.setSilent(true);
        sirenHead.setBaby(false);
        sirenHead.setAI(true);
        sirenHead.setPersistent(true);
        sirenHead.setRemoveWhenFarAway(false);
        // TODO: Replace with custom model via MythicMobs or ModelEngine
        return sirenHead;
    }
}