package eu.xaru.mysticrpg.content.classes;

import eu.xaru.mysticrpg.Main;
import eu.xaru.mysticrpg.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClassManager {
    private final Main plugin;
    private final Map<String, PlayerClass> classes = new HashMap<>();

    public ClassManager(Main plugin) {
        this.plugin = plugin;
        loadClasses();
    }

    public void loadClasses() {
        File classesFolder = new File(plugin.getDataFolder(), "classes");
        if (!classesFolder.exists()) {
            classesFolder.mkdirs();
        }

        File[] classFiles = classesFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (classFiles != null) {
            for (File classFile : classFiles) {
                try (FileReader reader = new FileReader(classFile)) {
                    JSONObject classData = (JSONObject) new JSONParser().parse(reader);
                    String className = classFile.getName().replace(".json", "");
                    classes.put(className, new PlayerClass(classData));
                } catch (IOException | ParseException e) {
                    Logger.error("Failed to load class " + classFile.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public PlayerClass getClass(String name) {
        return classes.get(name);
    }

    public Set<String> getClassNames() {
        return classes.keySet();
    }
}
