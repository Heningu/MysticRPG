package eu.xaru.mysticrpg.content.menus;

import eu.xaru.mysticrpg.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonReader {

    private final Map<String, MenuData> menus = new HashMap<>();
    private final Map<String, HotbarItem> hotbarItems = new HashMap<>();

    public void loadMenus(File menusFolder) {
        if (!menusFolder.exists() || !menusFolder.isDirectory()) {
            Logger.error("Menus folder does not exist or is not a directory.");
            return;
        }

        File[] menuFiles = menusFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (menuFiles != null) {
            for (File menuFile : menuFiles) {
                try (FileReader reader = new FileReader(menuFile)) {
                    JSONObject menuData = (JSONObject) new JSONParser().parse(reader);
                    parseMenuData(menuFile.getName().replace(".json", ""), menuData);
                } catch (IOException | ParseException e) {
                    Logger.error("Failed to load menu " + menuFile.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private void parseMenuData(String menuName, JSONObject menuData) {
        MenuData menu = new MenuData();
        menu.setMenuName(menuName);

        menu.setRows(getIntValue(menuData, "Rows", 1));
        menu.setColumns(getIntValue(menuData, "Columns", 9));
        menu.setFillRestWithWhiteGlass(getBooleanValue(menuData, "fill_rest_with_white_glass", false));

        JSONArray itemsArray = (JSONArray) menuData.get("items");
        if (itemsArray != null) {
            for (Object itemObj : itemsArray) {
                JSONObject itemData = (JSONObject) itemObj;
                MenuItem menuItem = new MenuItem();
                menuItem.setMaterial(getStringValue(itemData, "material", "STONE"));
                menuItem.setName(getStringValue(itemData, "name", ""));
                menuItem.setHideEnchants(getBooleanValue(itemData, "hideEnchants", false));
                menuItem.setDescription(getStringValue(itemData, "description", ""));
                menuItem.setRow(getIntValue(itemData, "row", 1));
                menuItem.setColumn(getIntValue(itemData, "column", 1));
                menuItem.setCommand(getStringValue(itemData, "command", ""));
                menu.getItems().add(menuItem);
            }
        }

        menus.put(menuName, menu);

        JSONObject hotbarItemData = (JSONObject) menuData.get("hotbar_item");
        if (hotbarItemData != null) {
            HotbarItem hotbarItem = new HotbarItem();
            hotbarItem.setUse(getBooleanValue(hotbarItemData, "use", false));
            hotbarItem.setName(getStringValue(hotbarItemData, "name", ""));
            hotbarItem.setItemDescription(getStringValue(hotbarItemData, "item_description", ""));
            hotbarItem.setHotbarSlot(getIntValue(hotbarItemData, "hotbar_slot", 1));
            hotbarItem.setMaterial(getStringValue(hotbarItemData, "material", "STONE"));
            hotbarItem.setHideEnchants(getBooleanValue(hotbarItemData, "hideEnchants", false));
            hotbarItem.setItemDropable(getBooleanValue(hotbarItemData, "item_dropable", false));
            hotbarItem.setItemMoveable(getBooleanValue(hotbarItemData, "item_moveable", false));
            hotbarItem.setKeepItemOnDeath(getBooleanValue(hotbarItemData, "keep_item_on_death", false));
            hotbarItem.setCommand(getStringValue(hotbarItemData, "command", ""));
            JSONObject enchantmentsData = (JSONObject) hotbarItemData.get("enchants");
            if (enchantmentsData != null) {
                Map<String, Integer> enchantments = new HashMap<>();
                for (Object key : enchantmentsData.keySet()) {
                    enchantments.put(key.toString(), getIntValue(enchantmentsData, key, 1));
                }
                hotbarItem.setEnchantments(enchantments);
            }
            hotbarItems.put(menuName, hotbarItem);
        }
    }

    private int getIntValue(JSONObject jsonObject, String key, int defaultValue) {
        Object value = jsonObject.get(key);
        return value instanceof Long ? ((Long) value).intValue() : defaultValue;
    }

    private int getIntValue(JSONObject jsonObject, Object key, int defaultValue) {
        Object value = jsonObject.get(key);
        return value instanceof Long ? ((Long) value).intValue() : defaultValue;
    }

    private boolean getBooleanValue(JSONObject jsonObject, String key, boolean defaultValue) {
        Object value = jsonObject.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private String getStringValue(JSONObject jsonObject, String key, String defaultValue) {
        Object value = jsonObject.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public Map<String, MenuData> getMenus() {
        return menus;
    }

    public Map<String, HotbarItem> getHotbarItems() {
        return hotbarItems;
    }
}
