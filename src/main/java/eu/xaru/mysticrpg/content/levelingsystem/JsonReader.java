package eu.xaru.mysticrpg.content.levelingsystem;

import eu.xaru.mysticrpg.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonReader {

    private final Map<Integer, LevelingSystem.Level> levels = new HashMap<>();
    private final String filePath = "plugins/MysticRPG/levelingsystem/levels.json";

    public void loadLevels() {
        File file = new File(filePath);
        if (!file.exists()) {
            createDefaultLevelsFile(file);
        }

        try (FileReader reader = new FileReader(file)) {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(reader);
            JSONObject levelsObject = (JSONObject) jsonObject.get("levels");

            for (Object key : levelsObject.keySet()) {
                int level = Integer.parseInt((String) key);
                JSONObject levelData = (JSONObject) levelsObject.get(key);

                LevelingSystem.Level levelInfo = new LevelingSystem.Level();
                levelInfo.setRequiredXp(((Long) levelData.get("required_xp")).intValue());
                levelInfo.setRewardCommand((String) levelData.get("reward_command"));

                levels.put(level, levelInfo);
            }

        } catch (IOException | ParseException e) {
            Logger.error("Failed to load levels configuration file: " + e.getMessage());
        }
    }

    private void createDefaultLevelsFile(File file) {
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        JSONObject jsonObject = new JSONObject();
        JSONObject levelsObject = new JSONObject();

        for (int i = 1; i <= 20; i++) {
            JSONObject levelData = new JSONObject();
            levelData.put("required_xp", i * 20); // Example XP requirements
            levelData.put("reward_command", "say Player has reached level " + i + "!");
            levelsObject.put(String.valueOf(i), levelData);
        }

        jsonObject.put("levels", levelsObject);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonObject.toJSONString());
        } catch (IOException e) {
            Logger.error("Failed to create default levels configuration file: " + e.getMessage());
        }
    }

    public Map<Integer, LevelingSystem.Level> getLevels() {
        return levels;
    }
}
