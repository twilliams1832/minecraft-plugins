package com.example.toiletplugin.listeners;

import com.example.toiletplugin.ToiletManager;
import com.example.toiletplugin.ToiletPlugin;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all right-click interactions with toilet blocks.
 *
 * Interaction map:
 *   RIGHT_CLICK on BOWL block  → Sit (if empty-handed) or Flush (if holding something)
 *   RIGHT_CLICK on SEAT block  → Toggle seat up/down (empty hand only)
 *   SNEAK + RIGHT_CLICK on BOWL → Stand up / dismount
 */
public class ToiletInteractListener implements Listener {

    private final ToiletPlugin plugin;
    private final ToiletManager manager;

    public ToiletInteractListener(ToiletPlugin plugin, ToiletManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only care about right-clicks on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Ignore off-hand events to avoid double-firing
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean emptyHand = heldItem.getType() == Material.AIR;

        if (manager.isToiletItem(heldItem)) {
            event.setCancelled(true);
            if (!player.hasPermission("toiletplugin.place")) {
                player.sendMessage("§cYou don't have permission to place toilets.");
                return;
            }

            if (manager.placeToiletOnTop(player, block) && player.getGameMode() != GameMode.CREATIVE) {
                heldItem.setAmount(heldItem.getAmount() - 1);
            }
            return;
        }

        if (manager.isExperimentalToiletItem(heldItem)) {
            event.setCancelled(true);
            if (!player.hasPermission("toiletplugin.place")) {
                player.sendMessage("§cYou don't have permission to place toilets.");
                return;
            }

            if (manager.placeToiletOnTop(player, block, true) && player.getGameMode() != GameMode.CREATIVE) {
                heldItem.setAmount(heldItem.getAmount() - 1);
            }
            return;
        }

        // --- Toilet Bowl interaction ---
        if (manager.isToiletBowl(block)) {
            event.setCancelled(true); // Prevent stair interaction (e.g. opening inventory)

            if (player.isSneaking() && manager.isPlayerSeated(player)) {
                // Sneak + right-click while seated → stand up
                manager.dismountPlayer(player);
                return;
            }

            if (manager.isPlayerSeated(player)) {
                // Already seated: flush instead
                manager.flushToilet(player, block);
                return;
            }

            if (emptyHand) {
                // Empty hand + not seated → sit down
                manager.sitOnToilet(player, block);
            } else {
                // Holding an item → flush
                manager.flushToilet(player, block);
            }
            return;
        }

        // --- Toilet Seat (trapdoor) interaction ---
        if (manager.isToiletSeat(block)) {
            event.setCancelled(true); // Prevent default trapdoor toggle

            if (emptyHand) {
                // Toggle seat open/closed with empty hand
                manager.toggleSeat(player, block);
            } else {
                player.sendMessage("§7Use an empty hand to toggle the seat.");
            }
        }
    }
}
