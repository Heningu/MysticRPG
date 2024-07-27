package eu.xaru.mysticrpg.hooks;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Main plugin;

    public PlaceholderHook(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true; // This is required for PlaceholderAPI to keep the expansion loaded
    }

    @Override
    public boolean canRegister() {
        return true; // This is required for PlaceholderAPI to register the expansion
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return "mysticrpg";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        PlayerData playerData = plugin.getLocalStorage().loadPlayerData(player.getUniqueId());

        if (identifier.equals("hp")) {
            return String.valueOf(playerData.getHp());
        }

        if (identifier.equals("max_hp")) {
            return String.valueOf(playerData.getMaxHp());
        }

        if (identifier.equals("mana")) {
            return String.valueOf(playerData.getMana());
        }

        if (identifier.equals("max_mana")) {
            return String.valueOf(playerData.getMaxMana());
        }

        if (identifier.equals("skillpoints")) {
            return String.valueOf(playerData.getSkillPoints());
        }

        if (identifier.equals("coins")) {
            return String.valueOf(playerData.getCoins());
        }

        return null; // Placeholder is unknown by the expansion
    }
}
