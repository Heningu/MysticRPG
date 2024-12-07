package eu.xaru.mysticrpg.player.stats;

import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.utils.DebugLogger;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerStatManager {

    private final PlayerDataCache playerDataCache;
    

    // Map to store temporary stat modifiers for each player
    private final Map<UUID, Map<String, Integer>> temporaryModifiers = Collections.synchronizedMap(new HashMap<>());

    public PlayerStatManager(PlayerDataCache playerDataCache) {
        this.playerDataCache = playerDataCache;
 
    }

    /**
     * Increases a player's attribute permanently.
     *
     * @param player       The player.
     * @param attribute    The attribute to increase.
     * @param amount       The amount to increase.
     */
    public void increaseAttributePermanent(Player player, String attribute, int amount) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return;
        }

        Map<String, Integer> attributes = data.getAttributes();
        attributes.put(attribute, attributes.getOrDefault(attribute, 0) + amount);
        data.setAttributes(attributes);

        DebugLogger.getInstance().log("Increased " + attribute + " permanently by " + amount + " for player " + player.getName());
    }

    /**
     * Decreases a player's attribute permanently.
     *
     * @param player       The player.
     * @param attribute    The attribute to decrease.
     * @param amount       The amount to decrease.
     */
    public void decreaseAttributePermanent(Player player, String attribute, int amount) {
        increaseAttributePermanent(player, attribute, -amount);
    }

    /**
     * Increases a player's attribute temporarily.
     *
     * @param player       The player.
     * @param attribute    The attribute to increase.
     * @param amount       The amount to increase.
     */
    public void increaseAttributeTemporary(Player player, String attribute, int amount) {
        UUID uuid = player.getUniqueId();
        temporaryModifiers.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<String, Integer> modifiers = temporaryModifiers.get(uuid);
        modifiers.put(attribute, modifiers.getOrDefault(attribute, 0) + amount);

        DebugLogger.getInstance().log("Increased " + attribute + " temporarily by " + amount + " for player " + player.getName());
    }

    /**
     * Decreases a player's attribute temporarily.
     *
     * @param player       The player.
     * @param attribute    The attribute to decrease.
     * @param amount       The amount to decrease.
     */
    public void decreaseAttributeTemporary(Player player, String attribute, int amount) {
        increaseAttributeTemporary(player, attribute, -amount);
    }

    /**
     * Clears all temporary modifiers for a player.
     *
     * @param player The player.
     */
    public void clearTemporaryModifiers(Player player) {
        temporaryModifiers.remove(player.getUniqueId());
        DebugLogger.getInstance().log("Cleared temporary modifiers for player " + player.getName());
    }

    /**
     * Gets the effective attribute value for a player, combining permanent and temporary modifiers.
     *
     * @param player    The player.
     * @param attribute The attribute to get.
     * @return The effective attribute value.
     */
    public int getEffectiveAttribute(Player player, String attribute) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return 0;
        }

        int baseValue = data.getAttributes().getOrDefault(attribute, 0);
        int tempModifier = temporaryModifiers.getOrDefault(player.getUniqueId(), Collections.emptyMap())
                .getOrDefault(attribute, 0);

        return baseValue + tempModifier;
    }

    /**
     * Gets all effective attributes for a player.
     *
     * @param player The player.
     * @return A map of attribute names to their effective values.
     */
    public Map<String, Integer> getAllEffectiveAttributes(Player player) {
        PlayerData data = playerDataCache.getCachedPlayerData(player.getUniqueId());
        if (data == null) {
            DebugLogger.getInstance().error("No cached data found for player: " + player.getName());
            return Collections.emptyMap();
        }

        Map<String, Integer> effectiveAttributes = new HashMap<>(data.getAttributes());
        Map<String, Integer> tempModifiers = temporaryModifiers.getOrDefault(player.getUniqueId(), Collections.emptyMap());

        for (Map.Entry<String, Integer> entry : tempModifiers.entrySet()) {
            effectiveAttributes.put(entry.getKey(), effectiveAttributes.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }

        return effectiveAttributes;
    }
}
