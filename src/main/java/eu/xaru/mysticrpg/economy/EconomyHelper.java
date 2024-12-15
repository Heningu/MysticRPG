package eu.xaru.mysticrpg.economy;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyHelper {
    private final PlayerDataCache playerDataCache;

    public EconomyHelper(PlayerDataCache playerDataCache) {
        this.playerDataCache = playerDataCache;
    }

    // Get held gold
    public int getHeldGold(Player player) {
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        return pd != null ? pd.getHeldGold() : 0;
    }

    // Set held gold
    public void setHeldGold(Player player, int amount) {
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (pd == null) return;
        pd.setHeldGold(Math.max(amount, 0));
    }

    // Add to held gold
    public void addHeldGold(Player player, int amount) {
        if (amount == 0) return;
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (pd == null) return;
        pd.setHeldGold(Math.max(pd.getHeldGold() + amount, 0));
    }

    // Get bank gold
    public int getBankGold(Player player) {
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        return pd != null ? pd.getBankGold() : 0;
    }

    // Set bank gold
    public void setBankGold(Player player, int amount) {
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (pd == null) return;
        pd.setBankGold(Math.max(amount, 0));
    }

    // Add to bank gold
    public void addBankGold(Player player, int amount) {
        if (amount == 0) return;
        PlayerData pd = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (pd == null) return;
        pd.setBankGold(Math.max(pd.getBankGold() + amount, 0));
    }

    // Deposit from held to bank
    public boolean depositToBank(Player player, int amount) {
        if (amount <= 0) {
            player.sendMessage(Utils.getInstance().$("Deposit amount must be positive."));
            return false;
        }

        int held = getHeldGold(player);
        if (held < amount) {
            player.sendMessage(Utils.getInstance().$("You don't have enough gold in your hands."));
            return false;
        }

        setHeldGold(player, held - amount);
        addBankGold(player, amount);
        player.sendMessage(Utils.getInstance().$("You deposited " + amount + " gold into the bank."));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }

    // Withdraw from bank to held
    public boolean withdrawFromBank(Player player, int amount) {
        if (amount <= 0) {
            player.sendMessage(Utils.getInstance().$("Withdrawal amount must be positive."));
            return false;
        }

        int bank = getBankGold(player);
        if (bank < amount) {
            player.sendMessage(Utils.getInstance().$("You don't have enough gold in the bank."));
            return false;
        }

        setBankGold(player, bank - amount);
        addHeldGold(player, amount);
        player.sendMessage(Utils.getInstance().$("You withdrew " + amount + " gold from the bank."));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }

    // For formatting
    public String formatGold(int amount) {
        return String.valueOf(amount);
    }
}

