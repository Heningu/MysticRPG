package eu.xaru.mysticrpg.stats;

import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;

public class StatManager {
    private final MysticCore plugin;
    private final PlayerDataManager playerDataManager;

    public StatManager(MysticCore plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void increaseAttribute(Player player, String attributeName) {
        plugin.getLogger().info("StatManager received attribute name: " + attributeName);

        PlayerData data = playerDataManager.getPlayerData(player);

        if (data.getAttributePoints() <= 0) {
            plugin.getLogger().info("Player " + player.getName() + " has no attribute points.");
            return;
        }

        switch (attributeName) {
            case "Increase Vitality":
                data.setVitality(data.getVitality() + 1);
                data.setHp(data.getHp() + 2); // Assuming each point of vitality adds 2 max HP
                plugin.getLogger().info("Player " + player.getName() + " increased Vitality to " + data.getVitality());
                break;
            case "Increase Intelligence":
                data.setIntelligence(data.getIntelligence() + 1);
                data.setMana(data.getMana() + 2); // Assuming each point of intelligence adds 2 max Mana
                plugin.getLogger().info("Player " + player.getName() + " increased Intelligence to " + data.getIntelligence());
                break;
            case "Increase Dexterity":
                data.setDexterity(data.getDexterity() + 1);
                data.setAttackDamageDex(data.getAttackDamageDex() + 1); // Assuming each point of dexterity adds 1 attack damage dex
                plugin.getLogger().info("Player " + player.getName() + " increased Dexterity to " + data.getDexterity());
                break;
            case "Increase Strength":
                data.setStrength(data.getStrength() + 1);
                data.setAttackDamage(data.getAttackDamage() + 1); // Assuming each point of strength adds 1 attack damage
                plugin.getLogger().info("Player " + player.getName() + " increased Strength to " + data.getStrength());
                break;
            default:
                plugin.getLogger().info("Unknown attribute name: " + attributeName);
                return;
        }

        data.setAttributePoints(data.getAttributePoints() - 1);
        playerDataManager.save(player);
    }
}
