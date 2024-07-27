package eu.xaru.mysticrpg.content.modules;

import eu.xaru.mysticrpg.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyModule implements Module {
    private final Main plugin;
    private final Map<UUID, Integer> balances = new HashMap<>();

    public EconomyModule(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean load() {
        // Add any logic to initialize the module here
        // For example, load balances from a file or database
        return true; // Ensure it returns a boolean
    }

    @Override
    public String getName() {
        return "EconomyModule";
    }

    public int getBalance(UUID playerUUID) {
        return balances.getOrDefault(playerUUID, 0);
    }

    public void setBalance(UUID playerUUID, int amount) {
        balances.put(playerUUID, amount);
    }

    public void addBalance(UUID playerUUID, int amount) {
        balances.put(playerUUID, getBalance(playerUUID) + amount);
    }

    public void subtractBalance(UUID playerUUID, int amount) {
        balances.put(playerUUID, getBalance(playerUUID) - amount);
    }

    public boolean hasEnough(UUID playerUUID, int amount) {
        return getBalance(playerUUID) >= amount;
    }
}
