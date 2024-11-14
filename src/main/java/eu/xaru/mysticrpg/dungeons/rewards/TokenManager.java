package eu.xaru.mysticrpg.dungeons.rewards;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenManager {

    private final Map<UUID, Integer> playerTokens;

    public TokenManager() {
        this.playerTokens = new HashMap<>();
    }

    public void addTokens(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        playerTokens.put(uuid, getTokens(player) + amount);
        player.sendMessage("You have earned " + amount + " tokens!");
    }

    public int getTokens(Player player) {
        return playerTokens.getOrDefault(player.getUniqueId(), 0);
    }

    // Methods to spend tokens, check balances, etc.
}
