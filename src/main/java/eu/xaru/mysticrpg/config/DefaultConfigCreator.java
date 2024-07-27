package eu.xaru.mysticrpg.config;

import eu.xaru.mysticrpg.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DefaultConfigCreator {
    private final Main plugin;

    public DefaultConfigCreator(Main plugin) {
        this.plugin = plugin;
    }

    public void createDefaultFiles() {
        createMenusFolder();
        createClassesFolder();
        createSystemsFolder();
    }

    private void createMenusFolder() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            try {
                File mainMenuFile = new File(menusFolder, "mainmenu.json");
                if (!mainMenuFile.exists()) {
                    FileWriter writer = new FileWriter(mainMenuFile);
                    writer.write("{\n" +
                            "  \"Menu_name\": \"Mainmenu\",\n" +
                            "  \"Rows\": 6,\n" +
                            "  \"Columns\": 9,\n" +
                            "  \"items\": [\n" +
                            "    {\n" +
                            "      \"material\": \"NETHERITE_CHESTPLATE\",\n" +
                            "      \"name\": \"&0[EQUIPMENT]\",\n" +
                            "      \"description\": \"&7Manage your equipment.\",\n" +
                            "      \"slot\": 1\n" +
                            "    }\n" +
                            "    // More items here...\n" +
                            "  ]\n" +
                            "}");
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createClassesFolder() {
        File classesFolder = new File(plugin.getDataFolder(), "classes");
        if (!classesFolder.exists()) {
            classesFolder.mkdirs();
            try {
                File defaultClassFile = new File(classesFolder, "default_class.json");
                if (!defaultClassFile.exists()) {
                    FileWriter writer = new FileWriter(defaultClassFile);
                    writer.write("{\n" +
                            "  \"class_name\": \"Default\",\n" +
                            "  \"material\": \"IRON_SWORD\",\n" +
                            "  \"description\": \"This is a default class.\"\n" +
                            "}");
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createSystemsFolder() {
        File systemsFolder = new File(plugin.getDataFolder(), "systems");
        if (!systemsFolder.exists()) {
            systemsFolder.mkdirs();
            try {
                File levelingSystemFile = new File(systemsFolder, "leveling.json");
                if (!levelingSystemFile.exists()) {
                    FileWriter writer = new FileWriter(levelingSystemFile);
                    writer.write("{\n" +
                            "  \"use_leveling_system\": true,\n" +
                            "  \"levels\": {\n" +
                            "    \"lvl1\": {\n" +
                            "      \"xp\": 0,\n" +
                            "      \"hp\": 10,\n" +
                            "      \"mana\": 10,\n" +
                            "      \"rewards\": {}\n" +
                            "    },\n" +
                            "    \"lvl2\": {\n" +
                            "      \"xp\": 10,\n" +
                            "      \"hp\": 12,\n" +
                            "      \"mana\": 9,\n" +
                            "      \"rewards\": {\n" +
                            "        \"commands\": [\n" +
                            "          \"/give %player% diamond 1\"\n" +
                            "        ],\n" +
                            "        \"permissions\": [\n" +
                            "          \"mysticrpg.special_ability\"\n" +
                            "        ]\n" +
                            "      }\n" +
                            "    },\n" +
                            "    \"lvl3\": {\n" +
                            "      \"xp\": 20,\n" +
                            "      \"hp\": 14,\n" +
                            "      \"mana\": 8,\n" +
                            "      \"rewards\": {}\n" +
                            "    }\n" +
                            "    // More levels here...\n" +
                            "  }\n" +
                            "}");
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
