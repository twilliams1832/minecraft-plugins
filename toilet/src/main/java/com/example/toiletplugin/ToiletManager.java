package com.example.toiletplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * ToiletManager manages the full lifecycle of all toilet instances:
 *  - Placement via /placetoilet
 *  - Sitting / dismounting
 *  - Flushing
 *  - Seat raising / lowering
 *  - Global cleanup on plugin disable
 */
public class ToiletManager {

    private static final String TOILET_BOWL_BLOCK_DATA = "minecraft:jigsaw[orientation=up_north]";
    private static final String TOILET_SEAT_DOWN_BLOCK_DATA = "minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]";
    private static final String TOILET_SEAT_UP_BLOCK_DATA = "minecraft:lightning_rod[facing=up,powered=true,waterlogged=false]";

    private final ToiletPlugin plugin;

    /**
     * Maps each bowl block location to its ToiletInstance.
     * Key is derived from Location using a stable string key.
     */
    private final Map<String, ToiletInstance> toilets = new HashMap<>();

    /**
     * Maps each seated player UUID to the toilet they are sitting on.
     * Enables fast lookup for quit/cleanup events.
     */
    private final Map<UUID, ToiletInstance> seatedPlayers = new HashMap<>();

    // Namespaced keys for PersistentDataContainer markers
    private final NamespacedKey IS_TOILET_BOWL_KEY;
    private final NamespacedKey IS_TOILET_SEAT_KEY;
    private final NamespacedKey TOILET_ITEM_KEY;
    private final NamespacedKey EXPERIMENTAL_TOILET_ITEM_KEY;

    public ToiletManager(ToiletPlugin plugin) {
        this.plugin = plugin;
        IS_TOILET_BOWL_KEY = new NamespacedKey(plugin, "is_toilet_bowl");
        IS_TOILET_SEAT_KEY = new NamespacedKey(plugin, "is_toilet_seat");
        TOILET_ITEM_KEY = new NamespacedKey(plugin, "toilet_item");
        EXPERIMENTAL_TOILET_ITEM_KEY = new NamespacedKey(plugin, "experimental_toilet_item");
    }

    // =========================================================================
    // Placement
    // =========================================================================

    /**
     * Gives the player a tagged toilet item that can be placed later.
     */
    public void giveToiletItem(Player player) {
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(createToiletItem());
        if (leftovers.isEmpty()) {
            player.sendMessage("§aYou received a toilet item. Right-click a block to place it.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.1f);
            return;
        }

        player.sendMessage("§cYour inventory is full.");
    }

    /**
     * Gives the player a tagged experimental toilet item.
     */
    public void giveExperimentalToiletItem(Player player) {
        HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(createExperimentalToiletItem());
        if (leftovers.isEmpty()) {
            player.sendMessage("§6You received an experimental toilet item. Right-click a block to place it.");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 0.8f);
            return;
        }

        player.sendMessage("§cYour inventory is full.");
    }

    /**
     * Creates the custom item that players use to place toilets.
     */
    public ItemStack createToiletItem() {
        ItemStack item = new ItemStack(Material.QUARTZ_STAIRS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§fToilet");
        meta.setLore(Arrays.asList(
            "§7Right-click a block to place.",
            "§7Includes bowl and seat."
        ));
        meta.setCustomModelData(1001);
        meta.getPersistentDataContainer().set(TOILET_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Creates the experimental toilet item that places placeholder blocks.
     */
    public ItemStack createExperimentalToiletItem() {
        ItemStack item = new ItemStack(Material.JIGSAW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§6Experimental Toilet");
        meta.setLore(Arrays.asList(
            "§7Right-click a block to place.",
            "§7Uses cross-platform placeholder blocks.",
            "§cMay have broken visuals or collision."
        ));
        meta.setCustomModelData(1002);
        meta.getPersistentDataContainer().set(EXPERIMENTAL_TOILET_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns true when the stack is one of this plugin's custom toilet items.
     */
    public boolean isToiletItem(ItemStack item) {
        if (item == null || item.getType() != Material.QUARTZ_STAIRS) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(TOILET_ITEM_KEY, PersistentDataType.BYTE);
    }

    /**
     * Returns true when the stack is one of this plugin's experimental toilet items.
     */
    public boolean isExperimentalToiletItem(ItemStack item) {
        if (item == null || item.getType() != Material.JIGSAW) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return meta.getPersistentDataContainer().has(EXPERIMENTAL_TOILET_ITEM_KEY, PersistentDataType.BYTE);
    }

    /**
     * Places a toilet at the block the player is targeting.
     * Structure:
     *   - Bowl: QUARTZ_STAIRS at target block
     *   - Seat: IRON_TRAPDOOR on top of the bowl block
     */
    public void placeToilet(Player player) {
        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            player.sendMessage("§cLook at a block to place the toilet on top of it.");
            return;
        }

        placeToiletOnTop(player, target);
    }

    /**
     * Places a toilet on the block directly above the clicked support block.
     *
     * @return true when the placement succeeds
     */
    public boolean placeToiletOnTop(Player player, Block target) {
        return placeToiletOnTop(player, target, false);
    }

    /**
     * Places either the stable or experimental toilet on the clicked support block.
     *
     * @return true when the placement succeeds
     */
    public boolean placeToiletOnTop(Player player, Block target, boolean experimental) {
        if (target == null) {
            player.sendMessage("§cLook at a block to place the toilet on top of it.");
            return false;
        }

        // Bowl goes on the block above the targeted surface
        Block bowlBlock = target.getRelative(BlockFace.UP);
        Block seatBlock = bowlBlock.getRelative(BlockFace.UP);

        // Check space is free
        if (!bowlBlock.getType().isAir() || !seatBlock.getType().isAir()) {
            player.sendMessage("§cNot enough space to place a toilet here.");
            return false;
        }

        if (experimental) {
            bowlBlock.setType(Material.JIGSAW, false);
            bowlBlock.setBlockData(Bukkit.createBlockData(TOILET_BOWL_BLOCK_DATA), false);
        } else {
            // --- Place bowl (QUARTZ_STAIRS) ---
            bowlBlock.setType(Material.QUARTZ_STAIRS);
            // Orient stairs to face the player for aesthetics
            Stairs stairsData = (Stairs) bowlBlock.getBlockData();
            stairsData.setFacing(player.getFacing().getOppositeFace());
            bowlBlock.setBlockData(stairsData);
        }

        // Tag the bowl block via PersistentDataContainer so we can identify it on interact
        markBlock(bowlBlock, IS_TOILET_BOWL_KEY);

        if (experimental) {
            seatBlock.setType(Material.LIGHTNING_ROD, false);
            seatBlock.setBlockData(Bukkit.createBlockData(TOILET_SEAT_DOWN_BLOCK_DATA), false);
        } else {
            // --- Place seat (IRON_TRAPDOOR) ---
            seatBlock.setType(Material.IRON_TRAPDOOR);
            // Start with seat down (closed trapdoor)
            Openable trapdoor = (Openable) seatBlock.getBlockData();
            trapdoor.setOpen(false);
            seatBlock.setBlockData(trapdoor);
        }

        // Tag the seat block
        markBlock(seatBlock, IS_TOILET_SEAT_KEY);

        // --- Register the toilet instance ---
        ToiletInstance instance = new ToiletInstance(bowlBlock.getLocation(), seatBlock.getLocation());
        String key = locationKey(bowlBlock.getLocation());
        toilets.put(key, instance);

        if (experimental) {
            player.sendMessage("§6Experimental toilet placed. Placeholder visuals and collision may be unstable.");
        } else {
            player.sendMessage("§aToilet placed! Right-click the bowl to flush, right-click the seat to raise/lower.");
        }
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 1.0f);
        return true;
    }

    // =========================================================================
    // Sitting
    // =========================================================================

    /**
     * Seats a player on the toilet bowl.
     * Spawns (or reuses) an invisible ArmorStand at the bowl center and mounts the player on it.
     */
    public void sitOnToilet(Player player, Block bowlBlock) {
        String key = locationKey(bowlBlock.getLocation());
        ToiletInstance instance = toilets.get(key);

        if (instance == null) {
            player.sendMessage("§cThis toilet is not registered.");
            return;
        }

        // Prevent double-sitting
        if (instance.isOccupied()) {
            player.sendMessage("§cSomeone is already using this toilet!");
            return;
        }

        if (seatedPlayers.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou are already sitting on a toilet.");
            return;
        }

        // Spawn ArmorStand if it doesn't exist yet (or was previously cleaned up)
        if (instance.getSeatArmorStand() == null || instance.getSeatArmorStand().isDead()) {
            ArmorStand stand = spawnSeatArmorStand(bowlBlock.getLocation());
            instance.setSeatArmorStand(stand);
        }

        // Mount the player on the ArmorStand
        instance.getSeatArmorStand().addPassenger(player);
        instance.setSeatedPlayerUUID(player.getUniqueId());
        seatedPlayers.put(player.getUniqueId(), instance);

        player.sendMessage("§7You sit down on the toilet. Take your time.");
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_PLACE, 0.5f, 0.8f);
    }

    /**
     * Dismounts a player from their toilet seat.
     * Called on quit or when they stand up manually.
     */
    public void dismountPlayer(Player player) {
        ToiletInstance instance = seatedPlayers.remove(player.getUniqueId());
        if (instance == null) return;

        ArmorStand stand = instance.getSeatArmorStand();
        if (stand != null && !stand.isDead()) {
            stand.removePassenger(player);
        }
        instance.clearSeat();
        player.sendMessage("§7You stand up.");
    }

    /**
     * Called by the interact listener when a player who is already seated
     * right-clicks the bowl (or presses sneak-interact to stand up).
     */
    public boolean isPlayerSeated(Player player) {
        return seatedPlayers.containsKey(player.getUniqueId());
    }

    // =========================================================================
    // Flushing
    // =========================================================================

    /**
     * Simulates a flush on the toilet bowl block.
     * Plays splash sounds and spawns water drip particles at the bowl center.
     */
    public void flushToilet(Player player, Block bowlBlock) {
        String key = locationKey(bowlBlock.getLocation());
        if (!toilets.containsKey(key)) {
            player.sendMessage("§cThis toilet is not registered.");
            return;
        }

        Location center = bowlBlock.getLocation().add(0.5, 0.5, 0.5);
        World world = bowlBlock.getWorld();

        // Play flush sound
        world.playSound(center, Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.2f);
        world.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.8f);

        // Spawn water splash particles in a small cluster
        world.spawnParticle(Particle.WATER_SPLASH, center, 30, 0.3, 0.1, 0.3, 0.1);
        world.spawnParticle(Particle.DRIP_WATER, center, 10, 0.2, 0.1, 0.2, 0);

        player.sendMessage("§bFwoooosh! 🚽");
    }

    // =========================================================================
    // Seat Toggle
    // =========================================================================

    /**
     * Toggles the iron trapdoor seat open/closed.
     * Open = seat up, Closed = seat down.
     */
    public void toggleSeat(Player player, Block seatBlock) {
        BlockData data = seatBlock.getBlockData();

        if (!(data instanceof Openable openable)) {
            player.sendMessage("§cThis seat block is invalid.");
            return;
        }

        boolean nowOpen = !openable.isOpen();
        openable.setOpen(nowOpen);
        seatBlock.setBlockData(openable);

        // Play trapdoor sound
        Sound sound = nowOpen ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
        seatBlock.getWorld().playSound(seatBlock.getLocation().add(0.5, 0.5, 0.5), sound, 1.0f, 1.0f);

        player.sendMessage(nowOpen ? "§7Seat raised. 🚽⬆" : "§7Seat lowered. 🚽⬇");
    }

    // =========================================================================
    // Identification helpers
    // =========================================================================

    /** Returns true if the block is a registered toilet bowl. */
    public boolean isToiletBowl(Block block) {
        return hasTag(block, IS_TOILET_BOWL_KEY);
    }

    /** Returns true if the block is a registered toilet seat. */
    public boolean isToiletSeat(Block block) {
        return hasTag(block, IS_TOILET_SEAT_KEY);
    }

    /** Retrieves the ToiletInstance for a given bowl block location, or null. */
    public ToiletInstance getInstanceByBowl(Block bowlBlock) {
        return toilets.get(locationKey(bowlBlock.getLocation()));
    }

    // =========================================================================
    // Cleanup
    // =========================================================================

    /**
     * Dismounts all players and removes all ArmorStands.
     * Called on plugin disable.
     */
    public void removeAllToilets() {
        // Dismount all seated players first
        for (UUID uuid : new HashSet<>(seatedPlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                dismountPlayer(player);
            }
        }
        seatedPlayers.clear();

        // Remove all ArmorStands
        for (ToiletInstance instance : toilets.values()) {
            ArmorStand stand = instance.getSeatArmorStand();
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        toilets.clear();
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Spawns an invisible, immovable ArmorStand at the center of the bowl block.
     * The player will be mounted on this stand when sitting.
     *
     * Offset: 0.5 on X/Z (block center), 0.1 on Y (slightly above floor).
     */
    private ArmorStand spawnSeatArmorStand(Location bowlLoc) {
        Location spawnLoc = bowlLoc.clone().add(0.5, 0.1, 0.5);
        World world = spawnLoc.getWorld();

        // Use the Consumer<ArmorStand> overload to configure before spawning (no tick delay)
        ArmorStand stand = world.spawn(spawnLoc, ArmorStand.class, as -> {
            as.setVisible(false);         // Invisible — players shouldn't see it
            as.setGravity(false);         // Stays in place
            as.setInvulnerable(true);     // Cannot be damaged
            as.setMarker(true);           // No hitbox — players won't accidentally click it
            as.setSmall(true);            // Small stand fits better in the block
            as.setCollidable(false);
        });
        return stand;
    }

    /**
     * Tags a block with a marker in its chunk's PersistentDataContainer equivalent.
     * We store the tag on a TileEntity-like approach: since most blocks lack PDC,
     * we encode the block position info into the plugin's in-memory map AND
     * also tag the block's chunk PDC for cross-reload identification.
     *
     * For simplicity, we use the chunk's PersistentDataContainer keyed by block coords.
     */
    private void markBlock(Block block, NamespacedKey key) {
        // Store a flag in the chunk's PDC using a position-encoded sub-key
        String posKey = block.getX() + "_" + block.getY() + "_" + block.getZ();
        NamespacedKey posNSKey = new NamespacedKey(plugin, key.getKey() + "_" + posKey);
        PersistentDataContainer chunkPDC = block.getChunk().getPersistentDataContainer();
        chunkPDC.set(posNSKey, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean hasTag(Block block, NamespacedKey key) {
        // Check both in-memory registry (fast) and chunk PDC (for persistence)
        String posKey = block.getX() + "_" + block.getY() + "_" + block.getZ();
        NamespacedKey posNSKey = new NamespacedKey(plugin, key.getKey() + "_" + posKey);
        PersistentDataContainer chunkPDC = block.getChunk().getPersistentDataContainer();
        return chunkPDC.has(posNSKey, PersistentDataType.BYTE);
    }

    /**
     * Generates a stable string key from a Location.
     * Format: "world:x:y:z"
     */
    private String locationKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public NamespacedKey getIsBowlKey() {
        return IS_TOILET_BOWL_KEY;
    }

    public NamespacedKey getIsSeatKey() {
        return IS_TOILET_SEAT_KEY;
    }
}
