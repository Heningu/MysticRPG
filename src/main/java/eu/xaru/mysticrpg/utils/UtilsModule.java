package eu.xaru.mysticrpg.utils;

import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilsModule implements IBaseModule {

    private static final boolean DEBUGGING_ENABLED = true;

    private static UtilsModule instance;
    private final Map<String, String> placeholders = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, String>> classPlaceholders = new ConcurrentHashMap<>();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Za-z0-9_-]+)}");
    private static final Pattern CONFIG_PATTERN = Pattern.compile("config:([A-Za-z0-9._-]+)");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");

    private DebugLoggerModule logger;

    private UtilsModule() {
        // Private constructor to enforce singleton pattern
    }

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        logger.log(Level.INFO, "UtilsModule initialized", 0);
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "UtilsModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "UtilsModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "UtilsModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();  // No dependencies as it should be a fundamental utility module
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.CRITICAL;
    }


    // Message utilities
    public String translateMessage(String message) {
        Matcher matcher = CONFIG_PATTERN.matcher(message);
        while (matcher.find()) {
            String configPath = matcher.group(1);
            message = message.replace(matcher.group(), "< - Config value not found - >"); // Simplified placeholder
        }
        return hexColor(message);
    }

    public String hexColor(String message) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String replaceSharp = hexCode.replace('#', 'x');

            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder();
            for (char c : ch) {
                builder.append("&").append(c);
            }
            message = message.replace(hexCode, builder.toString());
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Placeholder management
    public UtilsModule setPlaceholder(String key, String value) {
        placeholders.put(key, value);
        return this;
    }

    public UtilsModule setPlaceholders(Map<String, String> placeholders) {
        this.placeholders.putAll(placeholders);
        return this;
    }

    public UtilsModule setClassPlaceholders(Class<?> clazz, Map<String, String> placeholders) {
        classPlaceholders.put(clazz, new HashMap<>(placeholders));
        return this;
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

}
