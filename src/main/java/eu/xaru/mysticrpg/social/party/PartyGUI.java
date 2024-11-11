package eu.xaru.mysticrpg.social.party;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PartyGUI {

    private static final Map<UUID, Inventory> partyInventories = new HashMap<>();
    private static final Map<UUID, Map<Integer, UUID>> kickSlotMaps = new HashMap<>(); // Added mapping
    private static final MysticCore plugin = JavaPlugin.getPlugin(MysticCore.class);

    public static void openPartyGUI(Player player, PartyHelper partyHelper) {
        Inventory gui = Bukkit.createInventory(null, 45, "Party Menu");

        fillWithPlaceholders(gui);

        Party party = partyHelper.getParty(player.getUniqueId());

        int[] headSlots = {10, 19, 28}; // Slots for heads
        int index = 0;

        // Mapping from kick slots to member UUIDs
        Map<Integer, UUID> kickSlotToMemberMap = new HashMap<>();

        // Leader's head
        ItemStack leaderHead = getPlayerHead(player);
        ItemMeta leaderMeta = leaderHead.getItemMeta();
        leaderMeta.setDisplayName(Utils.getInstance().$( player.getName()));
        leaderHead.setItemMeta(leaderMeta);

        gui.setItem(headSlots[index], leaderHead);

        // Place placeholder next to leader's head
        gui.setItem(headSlots[index] + 1, getPlaceholder()); // Slot next to the head

        index++;

        if (party != null) {
            for (UUID memberUUID : party.getMembers()) {
                if (memberUUID.equals(player.getUniqueId())) continue;
                if (index >= headSlots.length) break;

                Player member = Bukkit.getPlayer(memberUUID);
                if (member == null) continue;

                ItemStack memberHead = getPlayerHead(member);
                ItemMeta memberMeta = memberHead.getItemMeta();
                memberMeta.setDisplayName(Utils.getInstance().$( member.getName()));
                memberHead.setItemMeta(memberMeta);

                gui.setItem(headSlots[index], memberHead);

                int kickSlot = headSlots[index] + 1; // Slot next to the head

                // Add kick button if player is leader
                if (party.getLeader().equals(player.getUniqueId())) {
                    ItemStack kickButton = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta kickMeta = kickButton.getItemMeta();
                    kickMeta.setDisplayName(Utils.getInstance().$( "Kick " + member.getName()));
                    kickButton.setItemMeta(kickMeta);
                    gui.setItem(kickSlot, kickButton);

                    // Store mapping from kick slot to member UUID
                    kickSlotToMemberMap.put(kickSlot, memberUUID);
                } else {
                    gui.setItem(kickSlot, getPlaceholder());
                }

                index++;
            }
        }

        // Fill remaining slots with skeleton skulls
        while (index < headSlots.length) {
            ItemStack skeletonSkull = new ItemStack(Material.SKELETON_SKULL);
            ItemMeta skullMeta = skeletonSkull.getItemMeta();
            skullMeta.setDisplayName(Utils.getInstance().$( "Empty Slot"));
            skeletonSkull.setItemMeta(skullMeta);

            gui.setItem(headSlots[index], skeletonSkull);
            gui.setItem(headSlots[index] + 1, getPlaceholder()); // Slot next to the skull

            index++;
        }

        partyInventories.put(player.getUniqueId(), gui);
        kickSlotMaps.put(player.getUniqueId(), kickSlotToMemberMap); // Store the kick slot mapping
        player.openInventory(gui);
    }

    private static void fillWithPlaceholders(Inventory inventory) {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);

        // Fill borders and specified slots with placeholders
        for (int i = 0; i < inventory.getSize(); i++) {
            if (
                    (i >= 0 && i <= 8) // Top row
                            || (i % 9 == 0) // Leftmost column
                            || (i % 9 == 8) // Rightmost column
                            || (i >= 36 && i <= 44) // Bottom row
            ) {
                inventory.setItem(i, placeholder);
            }
        }

        // Fill slots in rows 2 to 4 (indices 1 to 3) slots 4 to 8 (indices 3 to 7)
        for (int row = 1; row <= 3; row++) { // Rows 2 to 4
            for (int col = 3; col <= 7; col++) { // Slots 4 to 8
                int slot = row * 9 + col;
                inventory.setItem(slot, placeholder);
            }
        }
    }

    private static ItemStack getPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack getPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(" ");
        placeholder.setItemMeta(meta);
        return placeholder;
    }

    public static void handleInventoryClick(InventoryClickEvent event, PartyHelper partyHelper) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        if (!partyInventories.containsKey(player.getUniqueId())) return;
        if (!inventory.equals(partyInventories.get(player.getUniqueId()))) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        Party party = partyHelper.getParty(player.getUniqueId());
        if (party == null) return;

        int slot = event.getRawSlot();
        Map<Integer, UUID> kickSlotToMemberMap = kickSlotMaps.get(player.getUniqueId());

        if (kickSlotToMemberMap != null && kickSlotToMemberMap.containsKey(slot)) {
            if (!party.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(Utils.getInstance().$("You are not the party leader."));
                return;
            }

            UUID targetUUID = kickSlotToMemberMap.get(slot);
            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                partyHelper.kickPlayer(player, targetPlayer);
                openPartyGUI(player, partyHelper);
            } else {
                player.sendMessage(Utils.getInstance().$("Player is offline or not found."));
            }
        }
    }

    public static void handleInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        partyInventories.remove(player.getUniqueId());
        kickSlotMaps.remove(player.getUniqueId()); // Remove kick slot mapping when inventory closes
    }
}
