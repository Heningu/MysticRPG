package eu.xaru.mysticrpg.stats;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataManager;
import org.bukkit.entity.Player;

public class StatManager {
    private final Main plugin;
    private final PlayerDataManager playerDataManager;

    public StatManager(Main plugin, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
    }

    public void increaseVitality(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        if (data.getAttributePoints() > 0) {
            data.setVitality(data.getVitality() + 1);
            data.setHp(data.getHp() + 2); // Assuming each point of vitality adds 2 max HP
            data.setAttributePoints(data.getAttributePoints() - 1);
            playerDataManager.save(player);
        }
    }

    public void increaseIntelligence(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        if (data.getAttributePoints() > 0) {
            data.setIntelligence(data.getIntelligence() + 1);
            data.setMana(data.getMana() + 2); // Assuming each point of intelligence adds 2 max Mana
            data.setAttributePoints(data.getAttributePoints() - 1);
            playerDataManager.save(player);
        }
    }

    public void increaseDexterity(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        if (data.getAttributePoints() > 0) {
            data.setDexterity(data.getDexterity() + 1);
            data.setAttackDamageDex(data.getAttackDamageDex() + 1); // Assuming each point of dexterity adds 1 attack damage dex
            data.setAttributePoints(data.getAttributePoints() - 1);
            playerDataManager.save(player);
        }
    }

    public void increaseStrength(Player player) {
        PlayerData data = playerDataManager.getPlayerData(player);
        if (data.getAttributePoints() > 0) {
            data.setStrength(data.getStrength() + 1);
            data.setAttackDamage(data.getAttackDamage() + 1); // Assuming each point of strength adds 1 attack damage
            data.setAttributePoints(data.getAttributePoints() - 1);
            playerDataManager.save(player);
        }
    }
}
