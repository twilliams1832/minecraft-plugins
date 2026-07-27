package com.example.toiletplugin;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.UUID;

/**
 * ToiletInstance holds all state for a single placed toilet.
 *
 * A toilet consists of:
 *  - A bowl block (QUARTZ_STAIRS) — the main interactive block
 *  - A seat block (IRON_TRAPDOOR) — can be raised/lowered
 *  - An invisible ArmorStand — used as a seat vehicle for players
 *  - The UUID of the seated player (null if nobody is sitting)
 */
public class ToiletInstance {

    /** Location of the quartz stair bowl block */
    private final Location bowlLocation;

    /** Location of the iron trapdoor seat block */
    private final Location seatLocation;

    /**
     * The invisible ArmorStand that players mount when they sit.
     * Spawned at bowl center; removed on cleanup.
     * May be null before the first sit attempt.
     */
    private ArmorStand seatArmorStand;

    /** UUID of the player currently seated; null if unoccupied */
    private UUID seatedPlayerUUID;

    public ToiletInstance(Location bowlLocation, Location seatLocation) {
        // Store cloned locations to avoid external mutation
        this.bowlLocation = bowlLocation.clone();
        this.seatLocation = seatLocation.clone();
        this.seatArmorStand = null;
        this.seatedPlayerUUID = null;
    }

    public Location getBowlLocation() {
        return bowlLocation.clone();
    }

    public Location getSeatLocation() {
        return seatLocation.clone();
    }

    public ArmorStand getSeatArmorStand() {
        return seatArmorStand;
    }

    public UUID getSeatedPlayerUUID() {
        return seatedPlayerUUID;
    }

    /** Returns true if a player is currently seated on this toilet */
    public boolean isOccupied() {
        return seatedPlayerUUID != null;
    }

    public void setSeatArmorStand(ArmorStand stand) {
        this.seatArmorStand = stand;
    }

    public void setSeatedPlayerUUID(UUID uuid) {
        this.seatedPlayerUUID = uuid;
    }

    /**
     * Clears the seated player, leaving the toilet unoccupied.
     * Does NOT remove the ArmorStand (it persists for future sitters).
     */
    public void clearSeat() {
        this.seatedPlayerUUID = null;
    }
}
