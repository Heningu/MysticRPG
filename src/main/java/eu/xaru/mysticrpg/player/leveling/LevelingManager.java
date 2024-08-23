//package eu.xaru.mysticrpg.player.leveling;
//
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import eu.xaru.mysticrpg.storage.old_playerdata;
//import eu.xaru.mysticrpg.storage.PlayerDataManager;
//import eu.xaru.mysticrpg.player.stats.StatManager;
//import org.bukkit.Bukkit;
//import org.bukkit.entity.Player;
//
//import java.io.InputStreamReader;
//import java.lang.reflect.Type;
//import java.util.Collections;
//import java.util.Map;
//import java.util.logging.Logger;
//
//public class LevelingManager {
//    private final PlayerDataManager playerDataManager;
//    private final StatManager statManager;
//    private final Map<String, Integer> xpValues;
//    private final Map<Integer, LevelData> levelDataMap;
//    private final int maxLevel;
//    private final Logger logger;
//
//    public LevelingManager(PlayerDataManager playerDataManager, StatManager statManager) {
//        this.playerDataManager = playerDataManager;
//        this.statManager = statManager;
//        this.logger = Bukkit.getLogger();
//
//        Gson gson = new Gson();
//        Type xpValuesType = new TypeToken<Map<String, Integer>>() {}.getType();
//        Type levelDataType = new TypeToken<Map<Integer, LevelData>>() {}.getType();
//
//        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/leveling/XPValues.json"))) {
//            this.xpValues = gson.fromJson(reader, xpValuesType);
//            logger.info("XPValues.json loaded: " + this.xpValues);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to load XPValues.json", e);
//        }
//
//        try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/leveling/Levels.json"))) {
//            this.levelDataMap = gson.fromJson(reader, levelDataType);
//            logger.info("Levels.json loaded properly");
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to load Levels.json", e);
//        }
//
//        this.maxLevel = levelDataMap.keySet().stream().max(Integer::compare).orElse(100);
//    }
//
//    public void addXp(Player player, int amount) {
//        old_playerdata playerData = playerDataManager.getPlayerData(player);
//        if (playerData.getLevel() < maxLevel) {
//            int newXp = playerData.getXp() + amount;
//            playerData.setXp(newXp);
//
//            while (playerData.getLevel() < maxLevel && newXp >= getLevelThreshold(playerData.getLevel() + 1)) {
//                newXp -= getLevelThreshold(playerData.getLevel() + 1);
//                playerData.setLevel(playerData.getLevel() + 1);
//                playerData.setXp(newXp);
//
//                // Apply level up rewards
//                LevelData levelData = levelDataMap.get(playerData.getLevel());
//                levelData.getRewards().forEach((stat, value) -> applyReward(playerData, stat, value));
//
//                // Execute level up commands
//                String command = levelData.getCommand();
//                if (command != null) {
//                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command.replace("{player}", player.getName()));
//                }
//            }
//
//            playerDataManager.save(player);
//        }
//    }
//
//    private void applyReward(old_playerdata playerData, String stat, int value) {
//        switch (stat) {
//            case "HP":
//                playerData.setHp(playerData.getHp() + value);
//                break;
//            case "Strength":
//                playerData.setStrength(playerData.getStrength() + value);
//                break;
//            case "Mana":
//                playerData.setMana(playerData.getMana() + value);
//                break;
//            case "Wisdom":
//                playerData.setAttribute("Wisdom", playerData.getAttribute("Wisdom") + value);
//                break;
//            case "AttributePoints":
//                playerData.setAttributePoints(playerData.getAttributePoints() + value);
//                break;
//            // Add other attributes and stats dynamically
//            default:
//                playerData.setAttribute(stat, playerData.getAttribute(stat) + value);
//                break;
//        }
//    }
//
//    public void setXp(Player player, int amount) {
//        old_playerdata playerData = playerDataManager.getPlayerData(player);
//        playerData.setXp(amount);
//        playerDataManager.save(player);
//    }
//
//    public int getXp(Player player) {
//        old_playerdata playerData = playerDataManager.getPlayerData(player);
//        return playerData.getXp();
//    }
//
//    public int getXpForEntity(String entityType) {
//        return xpValues.getOrDefault(entityType, 0);
//    }
//
//    public int getMaxLevel() {
//        return maxLevel;
//    }
//
//    public int getLevelThreshold(int level) {
//        LevelData levelData = levelDataMap.get(level);
//        if (levelData != null) {
//            return levelData.getXpRequired();
//        } else {
//            return 0;
//        }
//    }
//
//    public boolean isSpecialLevel(int level) {
//        LevelData levelData = levelDataMap.get(level);
//        return levelData != null && levelData.isSpecial();
//    }
//
//    public Map<String, Integer> getLevelRewards(int level) {
//        LevelData levelData = levelDataMap.get(level);
//        return levelData != null ? levelData.getRewards() : Collections.emptyMap();
//    }
//
//    private static class LevelData {
//        private int xp_required;
//        private String command;
//        private Map<String, Integer> rewards;
//        private boolean special;
//
//        public int getXpRequired() {
//            return xp_required;
//        }
//
//        public String getCommand() {
//            return command;
//        }
//
//        public Map<String, Integer> getRewards() {
//            return rewards;
//        }
//
//        public boolean isSpecial() {
//            return special;
//        }
//    }
//}
