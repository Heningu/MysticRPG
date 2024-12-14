package eu.xaru.mysticrpg.player.interaction.trading;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an active trade between two players.
 */
public class TradeSession {
    private final Player player1;
    private final Player player2;

    private final List<ItemStack> player1Items = new ArrayList<>();
    private final List<ItemStack> player2Items = new ArrayList<>();

    private boolean player1Ready = false;
    private boolean player2Ready = false;

    public TradeSession(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public List<ItemStack> getPlayer1Items() {
        return player1Items;
    }

    public List<ItemStack> getPlayer2Items() {
        return player2Items;
    }

    public boolean isPlayerReady(Player player) {
        if (player.equals(player1)) {
            return player1Ready;
        } else if (player.equals(player2)) {
            return player2Ready;
        }
        return false;
    }

    public void setPlayerReady(Player player, boolean ready) {
        if (player.equals(player1)) {
            player1Ready = ready;
        } else if (player.equals(player2)) {
            player2Ready = ready;
        }
    }

    public boolean bothReady() {
        return player1Ready && player2Ready;
    }

    /**
     * Adds an item to the trade from the specified player.
     *
     * @param player The player adding the item.
     * @param item   The item to add.
     * @return True if the item was added successfully, false otherwise.
     */
    public boolean addItem(Player player, ItemStack item) {
        // Limit the number of items per player to 3 based on GUI design
        if (player.equals(player1)) {
            if (player1Items.size() >= 3) {
                player.sendMessage(ChatColor.RED + "You cannot offer more than 3 items.");
                return false;
            }
            player1Items.add(item);
        } else if (player.equals(player2)) {
            if (player2Items.size() >= 3) {
                player.sendMessage(ChatColor.RED + "You cannot offer more than 3 items.");
                return false;
            }
            player2Items.add(item);
        } else {
            return false;
        }

        // Reset readiness since trade has changed
        player1Ready = false;
        player2Ready = false;
        return true;
    }

    /**
     * Returns all traded items to their respective owners.
     */
    public void returnItems() {
        for (ItemStack item : player1Items) {
            player1.getInventory().addItem(item.clone());
        }
        for (ItemStack item : player2Items) {
            player2.getInventory().addItem(item.clone());
        }
        player1Items.clear();
        player2Items.clear();
    }
}
