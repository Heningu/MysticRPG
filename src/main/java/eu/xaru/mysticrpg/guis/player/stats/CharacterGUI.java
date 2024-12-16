package eu.xaru.mysticrpg.guis.player.stats;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.StatsModule;
import eu.xaru.mysticrpg.quests.QuestModule;
import eu.xaru.mysticrpg.social.friends.FriendsModule;
import eu.xaru.mysticrpg.social.party.PartyModule;
import eu.xaru.mysticrpg.storage.PlayerData;
import eu.xaru.mysticrpg.storage.PlayerDataCache;
import eu.xaru.mysticrpg.storage.SaveModule;
import eu.xaru.mysticrpg.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.Map;
import java.util.UUID;

/**
 * Refactored to use StatsModule and PlayerStatsManager
 */
public class CharacterGUI {

    private final PlayerDataCache playerDataCache;
    private final StatsModule statsModule;
    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;

    public CharacterGUI() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = PlayerDataCache.getInstance();
        this.statsModule = ModuleManager.getInstance().getModuleInstance(StatsModule.class);
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.questModule = ModuleManager.getInstance().getModuleInstance(QuestModule.class);
        this.friendsModule = ModuleManager.getInstance().getModuleInstance(FriendsModule.class);
        this.partyModule = ModuleManager.getInstance().getModuleInstance(PartyModule.class);
    }

    public void openCharacterGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        PlayerData data = playerDataCache.getCachedPlayerData(playerUUID);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "Failed to retrieve your player data.");
            return;
        }

        Map<String, Integer> attributes = data.getAttributes();
        int attributePoints = data.getAttributePoints();

        int health = attributes.getOrDefault("HEALTH", 20);
        int defense = attributes.getOrDefault("DEFENSE", 0);
        int strength = attributes.getOrDefault("STRENGTH", 1);
        int intelligence = attributes.getOrDefault("INTELLIGENCE", 1);

        Item playerHead = createPlayerHead(player, health, defense, strength, intelligence);
        Item healthItem = createStatItem("Health", health, "Increases your maximum HP.");
        Item defenseItem = createStatItem("Defense", defense, "Reduces incoming damage.");
        Item strengthItem = createStatItem("Strength", strength, "Increases your damage output.");
        Item intelligenceItem = createStatItem("Intelligence", intelligence, "Increases your mana or magic damage.");

        Item healthButton = createIncrementButton(attributePoints, "HEALTH", player);
        Item defenseButton = createIncrementButton(attributePoints, "DEFENSE", player);
        Item strengthButton = createIncrementButton(attributePoints, "STRENGTH", player);
        Item intelligenceButton = createIncrementButton(attributePoints, "INTELLIGENCE", player);

        Item attributePointsItem = new SimpleItem(new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(ChatColor.YELLOW + "Attribute Points")
                .addLoreLines(ChatColor.WHITE + "Points: " + attributePoints)
                .addAllItemFlags());

        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to go back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true)) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule,statsModule, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };

        Item filler = new SimpleItem(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());

        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # X # # # #",
                        "# h # i # d # s #",
                        "# H # I # D # ^ #",
                        "# # # # # # # # #",
                        "# # # # # # # # #",
                        "B # # # # # # # A"
                )
                .addIngredient('#', filler)
                .addIngredient('X', playerHead)
                .addIngredient('h', healthItem)
                .addIngredient('i', intelligenceItem)
                .addIngredient('d', defenseItem)
                .addIngredient('s', strengthItem)
                .addIngredient('H', healthButton)
                .addIngredient('I', intelligenceButton)
                .addIngredient('D', defenseButton)
                .addIngredient('S', strengthButton)
                .addIngredient('A', attributePointsItem)
                .addIngredient('B', back)
                .build();

        gui.setItem(19, healthButton);
        gui.setItem(21, intelligenceButton);
        gui.setItem(23, defenseButton);
        gui.setItem(25, strengthButton);

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.GREEN + "Player Stats")
                .setGui(gui)
                .build();
        window.open();
    }

    private Item createPlayerHead(Player player, int health, int defense, int strength, int intelligence) {
        ItemStack headStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) headStack.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(ChatColor.GREEN + player.getName());
        skullMeta.setLore(java.util.Arrays.asList(
                ChatColor.YELLOW + "Health: " + ChatColor.WHITE + health,
                ChatColor.YELLOW + "Defense: " + ChatColor.WHITE + defense,
                ChatColor.YELLOW + "Strength: " + ChatColor.WHITE + strength,
                ChatColor.YELLOW + "Intelligence: " + ChatColor.WHITE + intelligence
        ));
        skullMeta.addItemFlags(ItemFlag.values());
        headStack.setItemMeta(skullMeta);

        return new SimpleItem(headStack);
    }

    private Item createStatItem(String name, int value, String description) {
        Material mat;
        switch (name.toUpperCase()) {
            case "HEALTH": mat = Material.REDSTONE; break;
            case "DEFENSE": mat = Material.SHIELD; break;
            case "STRENGTH": mat = Material.IRON_SWORD; break;
            case "INTELLIGENCE": mat = Material.ENCHANTED_BOOK; break;
            default: mat = Material.BARRIER; break;
        }

        return new SimpleItem(new ItemBuilder(mat)
                .setDisplayName(ChatColor.AQUA + name)
                .addLoreLines(
                        ChatColor.GRAY + description,
                        "",
                        ChatColor.YELLOW + "Current " + name + ": " + ChatColor.WHITE + value
                )
                .addAllItemFlags());
    }

    private Item createIncrementButton(int attributePoints, String attributeName, Player player) {
        boolean canIncrease = attributePoints > 0;
        Material buttonMaterial = canIncrease ? Material.SUNFLOWER : Material.BEDROCK;
        ChatColor color = canIncrease ? ChatColor.GREEN : ChatColor.RED;
        String displayName = color + "Increase " + attributeName;

        return new SimpleItem(new ItemBuilder(buttonMaterial)
                .setDisplayName(displayName)
                .addLoreLines(canIncrease ? ChatColor.GRAY + "Click to spend 1 attribute point" : ChatColor.RED + "No attribute points available")
                .addAllItemFlags()) {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                if (!canIncrease) {
                    clickPlayer.sendMessage(ChatColor.RED + "You have no attribute points left!");
                    return;
                }

                if (statsModule != null) {
                    statsModule.getStatsManager().increaseBaseAttribute(clickPlayer, attributeName);
                    openCharacterGUI(clickPlayer);
                } else {
                    clickPlayer.sendMessage(ChatColor.RED + "Stats module not found.");
                }
            }
        };
    }
}
