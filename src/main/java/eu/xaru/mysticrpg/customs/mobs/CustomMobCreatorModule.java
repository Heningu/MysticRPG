/*package eu.xaru.mysticrpg.customs.mobs;

import eu.xaru.mysticrpg.config.ConfigCreator;
import eu.xaru.mysticrpg.cores.MysticCore;
import eu.xaru.mysticrpg.enums.EModulePriority;
import eu.xaru.mysticrpg.interfaces.IBaseModule;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.utils.DebugLoggerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CustomMobCreatorModule implements IBaseModule, Listener {

    private DebugLoggerModule logger;
    private final Map<String, Map<String, Object>> customMobs = new HashMap<>();
    private FileConfiguration config;
    private File configFile;

    @Override
    public void initialize() {
        logger = ModuleManager.getInstance().getModuleInstance(DebugLoggerModule.class);
        logger.initialize();
        logger.log(Level.INFO, "CustomMobCreatorModule initialized", 0);

        loadMobsFromConfig();
        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(MysticCore.class));
    }

    @Override
    public void start() {
        logger.log(Level.INFO, "CustomMobCreatorModule started", 0);
    }

    @Override
    public void stop() {
        logger.log(Level.INFO, "CustomMobCreatorModule stopped", 0);
    }

    @Override
    public void unload() {
        logger.log(Level.INFO, "CustomMobCreatorModule unloaded", 0);
    }

    @Override
    public List<Class<? extends IBaseModule>> getDependencies() {
        return List.of();  // Adjust dependencies if needed
    }

    @Override
    public EModulePriority getPriority() {
        return EModulePriority.NORMAL;
    }

    private void loadMobsFromConfig() {
        File pluginFolder = Bukkit.getPluginManager().getPlugin("MysticRPG").getDataFolder();
        File mobsFolder = new File(pluginFolder, "mobs");

        configFile = new File(mobsFolder, "custommobs.yml");

        if (!configFile.exists()) {
            logger.error("custommobs.yml file not found. Make sure it was created correctly.", null, null);
            return;
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load the mobs from the configuration without spawning them
        for (String key : config.getKeys(false)) {
            try {
                Map<String, Object> mobData = new HashMap<>();
                mobData.put("type", config.getString(key + ".type"));
                mobData.put("name", config.getString(key + ".name"));
                mobData.put("level", config.getInt(key + ".level"));
                mobData.put("health", config.getDouble(key + ".health"));
                mobData.put("damage", config.getDouble(key + ".damage"));
                mobData.put("speed", config.getDouble(key + ".speed"));
                mobData.put("knockback_resistance", config.getDouble(key + ".knockback_resistance"));
                mobData.put("head", config.getItemStack(key + ".head"));
                mobData.put("helmet", config.getItemStack(key + ".equipment.helmet"));
                mobData.put("chestplate", config.getItemStack(key + ".equipment.chestplate"));
                mobData.put("leggings", config.getItemStack(key + ".equipment.leggings"));
                mobData.put("boots", config.getItemStack(key + ".equipment.boots"));
                mobData.put("weapon", config.getItemStack(key + ".equipment.weapon"));

                customMobs.put(key, mobData);

                logger.log(Level.INFO, "Loaded mob configuration: " + key, 0);
            } catch (Exception e) {
                logger.error("Error loading mob from config: " + key, e, null);
            }
        }

        logger.log(Level.INFO, "Loaded " + customMobs.size() + " custom mob configurations.", 0);
    }

    public void openMobGUI(Player player) {
        Inventory mobInventory = Bukkit.createInventory(null, 54, ChatColor.GREEN + "Custom Mobs");

        for (String key : customMobs.keySet()) {
            ItemStack mobHead = getMobHead(key);
            ItemMeta meta = mobHead.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + key);
                mobHead.setItemMeta(meta);
            }
            mobInventory.addItem(mobHead);
        }

        player.openInventory(mobInventory);
    }

    private ItemStack getMobHead(String mobKey) {
        ItemStack headItem = (ItemStack) customMobs.get(mobKey).get("head");
        return (headItem != null) ? headItem : new ItemStack(Material.ZOMBIE_HEAD);
    }

    public void openMobEquipmentGUI(Player player, String mobKey) {
        Map<String, Object> mobData = customMobs.get(mobKey);
        if (mobData == null) {
            player.sendMessage(ChatColor.RED + "Mob not found!");
            return;
        }

        Inventory equipmentInventory = Bukkit.createInventory(null, 27, ChatColor.GREEN + (String) mobData.get("name") + " Equipment");

        equipmentInventory.setItem(0, (ItemStack) mobData.get("helmet"));
        equipmentInventory.setItem(1, (ItemStack) mobData.get("chestplate"));
        equipmentInventory.setItem(2, (ItemStack) mobData.get("leggings"));
        equipmentInventory.setItem(3, (ItemStack) mobData.get("boots"));
        equipmentInventory.setItem(4, (ItemStack) mobData.get("weapon"));

        player.openInventory(equipmentInventory);
        player.setMetadata("editingMobKey", new FixedMetadataValue(JavaPlugin.getPlugin(MysticCore.class), mobKey));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("Equipment")) {
            Player player = (Player) event.getWhoClicked();
            String mobKey = player.getMetadata("editingMobKey").get(0).asString();

            if (mobKey != null) {
                int slot = event.getRawSlot();
                if (slot >= 0 && slot <= 4) {
                    event.setCancelled(false);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().contains("Equipment")) {
            Player player = (Player) event.getPlayer();
            String mobKey = player.getMetadata("editingMobKey").get(0).asString();
            Inventory inventory = event.getInventory();

            if (mobKey != null && inventory != null) {
                customMobs.get(mobKey).put("helmet", inventory.getItem(0));
                customMobs.get(mobKey).put("chestplate", inventory.getItem(1));
                customMobs.get(mobKey).put("leggings", inventory.getItem(2));
                customMobs.get(mobKey).put("boots", inventory.getItem(3));
                customMobs.get(mobKey).put("weapon", inventory.getItem(4));

                config.set(mobKey + ".equipment.helmet", inventory.getItem(0));
                config.set(mobKey + ".equipment.chestplate", inventory.getItem(1));
                config.set(mobKey + ".equipment.leggings", inventory.getItem(2));
                config.set(mobKey + ".equipment.boots", inventory.getItem(3));
                config.set(mobKey + ".equipment.weapon", inventory.getItem(4));

                try {
                    config.save(configFile);
                } catch (IOException e) {
                    logger.error("Could not save custom mob config for " + mobKey, e, null);
                }
            }
        }
    }

    public void spawnCustomMob(Player player, String mobKey) {
        Map<String, Object> mobData = customMobs.get(mobKey);
        if (mobData == null) {
            player.sendMessage(ChatColor.RED + "Mob configuration not found!");
            return;
        }

        EntityType entityType = EntityType.valueOf((String) mobData.get("type"));
        LivingEntity mob = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), entityType);

        mob.setCustomName((String) mobData.get("name"));
        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue((Double) mobData.get("health"));
        mob.setHealth((Double) mobData.get("health"));
        mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue((Double) mobData.get("damage"));
        mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue((Double) mobData.get("speed"));
        mob.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue((Double) mobData.get("knockback_resistance"));

        mob.getEquipment().setHelmet((ItemStack) mobData.get("helmet"));
        mob.getEquipment().setChestplate((ItemStack) mobData.get("chestplate"));
        mob.getEquipment().setLeggings((ItemStack) mobData.get("leggings"));
        mob.getEquipment().setBoots((ItemStack) mobData.get("boots"));
        mob.getEquipment().setItemInMainHand((ItemStack) mobData.get("weapon"));
    }
}*/
