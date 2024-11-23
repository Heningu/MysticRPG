package eu.xaru.mysticrpg.utils;

import eu.xaru.mysticrpg.cores.MysticCore;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class with thread-safe Singleton implementation.
 */
public class Utils {
    private static final boolean DEBUGGING_ENABLED = true;

    // Using cache only for config values
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private final Map<String, String> placeholders = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, String>> classPlaceholders = new ConcurrentHashMap<>();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z0-9_-]+)}");
    private static final Pattern CONFIG_PATTERN = Pattern.compile("config:([A-Za-z0-9._-]+)");
    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern FIX2_PATTERN = Pattern.compile("\\{#[0-9a-fA-F]{6}}");
    private static final Pattern FIX3_PATTERN = Pattern.compile("&x[&0-9a-fA-F]{12}");
    private static final Pattern GRADIENT1_PATTERN = Pattern.compile("<#[0-9a-fA-F]{6}>[^<]*</#[0-9a-fA-F]{6}>");
    private static final Pattern GRADIENT2_PATTERN = Pattern.compile("\\{#[0-9a-fA-F]{6}>}[^{]*\\{#[0-9a-fA-F]{6}<}");

    private Utils() {
        if (InstanceHolder.instance != null) {
            throw new IllegalStateException("Instance already created!");
        }
    }

    private static final class InstanceHolder {
        private static final Utils instance = new Utils();
    }

    public static Utils getInstance() {
        return InstanceHolder.instance;
    }

    // Simplified method for getting a config value with caching
    public String getConfigValue(String path) {
        return configCache.computeIfAbsent(path, p -> {
            String value = MysticCore.getInstance().getConfig().getString(p);
            return value != null ? value : "< - Error: Config value not found - >";
        });
    }

    public String $(String message) {
        return translateMessage(message);
    }

    // Updated translateMessage method to use the optimized caching
    public String translateMessage(String message) {
        Matcher matcher = CONFIG_PATTERN.matcher(message);
        while (matcher.find()) {
            String configPath = matcher.group(1);
            message = message.replace(matcher.group(), getConfigValue(configPath));
        }
        message = applyPlaceholders(message);
        message = hexColor(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    // Apply placeholders
    private String applyPlaceholders(String message) {
        Map<String, String> combinedPlaceholders = new HashMap<>(placeholders);
        Map<String, String> classPlaceholders = this.classPlaceholders
                .getOrDefault(MysticCore.getInstance().getClass(), Map.of());
        combinedPlaceholders.putAll(classPlaceholders);

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        while (matcher.find()) {
            String placeholderKey = matcher.group(1);
            String placeholderValue = combinedPlaceholders.getOrDefault(placeholderKey, "");
            message = message.replace(matcher.group(), placeholderValue);
        }
        return message;
    }

    // Serialization methods
    public String itemStackToBase64(ItemStack item) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return Base64.getEncoder().encodeToString(config.saveToString().getBytes());
    }

    public ItemStack itemStackFromBase64(String data) throws IOException, InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        byte[] decodedData = Base64.getDecoder().decode(data);
        config.loadFromString(new String(decodedData));
        return config.getItemStack("item");
    }


    // Combined hexColor method with full color formatting
    public String hexColor(String textInput) {
        String text = applyFormats(textInput);
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group();
            text = text.replace(hexCode, toChatColor(hexCode));
        }
        return text;
    }

    // Method to convert hex code to Minecraft ChatColor format
    private String toChatColor(String hexCode) {
        StringBuilder magic = new StringBuilder("§x");
        char[] characters = hexCode.substring(1).toCharArray();
        for (char c : characters) {
            magic.append('§').append(c);
        }
        return magic.toString();
    }

    // Apply various formats to the text
    private String applyFormats(String textInput) {
        String text = textInput;
        text = fixFormat1(text);
        text = fixFormat2(text);
        text = fixFormat3(text);
        text = setGradient1(text);
        text = setGradient2(text);
        return text;
    }

    // Fix format &#RRGGBB
    private String fixFormat1(String text) {
        return text.replace("&#", "#");
    }

    // Fix format {#RRGGBB}
    private String fixFormat2(String input) {
        String text = input;
        Matcher matcher = FIX2_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group();
            String fixed = hexCode.substring(2, 8);
            text = text.replace(hexCode, "#" + fixed);
        }
        return text;
    }

    // Fix format &x&R&R&G&G&B&B
    private String fixFormat3(String text) {
        text = text.replace('§', '&');
        Matcher matcher = FIX3_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group();
            String fixed = "" + hexCode.charAt(3) + hexCode.charAt(5) + hexCode.charAt(7)
                    + hexCode.charAt(9) + hexCode.charAt(11) + hexCode.charAt(13);
            text = text.replace(hexCode, "#" + fixed);
        }
        return text;
    }

    // Handle gradient format <#RRGGBB>Text</#RRGGBB>
    private String setGradient1(String input) {
        String text = input;
        Matcher matcher = GRADIENT1_PATTERN.matcher(text);
        while (matcher.find()) {
            String format = matcher.group();
            String startColor = format.substring(2, 8);
            String message = format.substring(9, format.length() - 10);
            String endColor = format.substring(format.length() - 7, format.length() - 1);
            String applied = asGradient(new TextColor(startColor), message, new TextColor(endColor));
            text = text.replace(format, applied);
        }
        return text;
    }

    // Handle gradient format {#RRGGBB>}text{#RRGGBB<}
    private String setGradient2(String input) {
        String text = input;
        Matcher matcher = GRADIENT2_PATTERN.matcher(text);
        while (matcher.find()) {
            String format = matcher.group();
            String startColor = format.substring(2, 8);
            String message = format.substring(10, format.length() - 10);
            String endColor = format.substring(format.length() - 8, format.length() - 2);
            String applied = asGradient(new TextColor(startColor), message, new TextColor(endColor));
            text = text.replace(format, applied);
        }
        return text;
    }

    // Apply a gradient between two colors over the text
    private String asGradient(TextColor start, String text, TextColor end) {
        StringBuilder sb = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            int red = (int) (start.red + (float) (end.red - start.red) / (length - 1) * i);
            int green = (int) (start.green + (float) (end.green - start.green) / (length - 1) * i);
            int blue = (int) (start.blue + (float) (end.blue - start.blue) / (length - 1) * i);
            sb.append(toChatColor("#" + toHexString(red, green, blue))).append(text.charAt(i));
        }
        return sb.toString();
    }

    // Convert RGB values to a hex string
    private String toHexString(int red, int green, int blue) {
        String hex = Integer.toHexString((red << 16) + (green << 8) + blue);
        while (hex.length() < 6) {
            hex = "0" + hex;
        }
        return hex;
    }

    // Inner class to represent an RGB color
    private static class TextColor {
        int red, green, blue;

        TextColor(String hex) {
            this.red = Integer.parseInt(hex.substring(0, 2), 16);
            this.green = Integer.parseInt(hex.substring(2, 4), 16);
            this.blue = Integer.parseInt(hex.substring(4, 6), 16);
        }
    }

    // Location utilities
    public boolean teleportTo(World world, Player player, double x, double z) {
        String worldName = world.getName();
        Location location;

        switch (worldName) {
            case "world", "world_the_end" -> {
                location = getProperLocationOverWorldEnd(world, player, x, z);
                return player.teleport(location);
            }
            case "world_nether" -> {
                location = getProperLocationNether(world, x, 3, z);
                if (location == null) {
                    return false;
                }
                return player.teleport(location);
            }
            default -> {
                return false;
            }
        }
    }

    private Location getProperLocationOverWorldEnd(World world, Player player, double x, double z) {
        Block block = world.getHighestBlockAt((int) x, (int) z);
        return new Location(world, x, block.getY(), z);
    }

    private Location getProperLocationNether(World world, double x, double y, double z) {
        int y2 = (int) y;
        Location location = null;
        Block centerBlock;
        while (y2 <= 120) {
            centerBlock = world.getBlockAt((int) x, y2, (int) z);
            if (chkRelativeBlock(centerBlock, BlockFace.SELF, 0)) {
                location = new Location(world, x, y2 + 2, z);
                break;
            }
            y2++;
        }
        return location;
    }

    private boolean chkRelativeBlock(Block block, BlockFace face, int distance) {
        Block relativeBlock = block.getRelative(face, distance);
        Block footBlock = relativeBlock.getRelative(BlockFace.UP);
        Block headBlock = relativeBlock.getRelative(BlockFace.UP, 2);

        return !relativeBlock.isLiquid() && !relativeBlock.isEmpty() && footBlock.isEmpty() && headBlock.isEmpty();
    }

    // Item utilities
    public ItemStack createItem(String name, Material material, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean checkUniqueInventory(InventoryClickEvent clickEvent, InventoryHolder inventoryHolder) {
        return Objects.equals(clickEvent.getInventory().getHolder(), inventoryHolder);
    }

    // Random utilities
    public int getRandomNumberInRange(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public String reverseMessage(String message) {
        return new StringBuilder(message).reverse().toString();
    }

    public Utils setPlaceholder(String key, String value) {
        placeholders.put(key, value);
        return this;
    }

    public Utils setPlaceholders(Map<String, String> placeholders) {
        this.placeholders.putAll(placeholders);
        return this;
    }

    public Utils setClassPlaceholders(Class<?> clazz, Map<String, String> placeholders) {
        classPlaceholders.put(clazz, new HashMap<>(placeholders));
        return this;
    }

    public Utils setClassPlaceholders(Class<?> clazz, String key, String value) {
        classPlaceholders.put(clazz, Map.of(key, value));
        return this;
    }
}
