package eu.xaru.mysticrpg.guis.player.stats;

import eu.xaru.mysticrpg.auctionhouse.AuctionHouseModule;
import eu.xaru.mysticrpg.guis.MainMenu;
import eu.xaru.mysticrpg.managers.ModuleManager;
import eu.xaru.mysticrpg.player.equipment.EquipmentModule;
import eu.xaru.mysticrpg.player.leveling.LevelModule;
import eu.xaru.mysticrpg.player.stats.PlayerStatModule;
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

public class CharacterGUI {

    private final PlayerDataCache playerDataCache;
    private final PlayerStatModule playerStatModule;
    private final AuctionHouseModule auctionHouse;
    private final EquipmentModule equipmentModule;
    private final LevelModule levelingModule;
    private final PlayerStatModule playerStat;
    private final QuestModule questModule;
    private final FriendsModule friendsModule;
    private final PartyModule partyModule;

    // Slots layout (9x6)
    // Row 0: # # # # H # # # #
    // Row 1: # V # I # D # S #
    // Row 2: # v # i # d # s #
    // Row 3: # # # # # # # # #
    // Row 4: # # # # # # # # #
    // Row 5: # # # # # # # # A
    // H = Player Head (4)
    // V = Vitality (10), v = Increase Vitality (19)
    // I = Intelligence (12), i = Increase Intelligence (21)
    // D = Dexterity (14), d = Increase Dexterity (23)
    // S = Strength (16), s = Increase Strength (25)
    // A = Attribute Points (53)
    // '#' are fillers

    public CharacterGUI() {
        SaveModule saveModule = ModuleManager.getInstance().getModuleInstance(SaveModule.class);
        this.playerDataCache = saveModule.getPlayerDataCache();
        this.playerStatModule = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
        this.auctionHouse = ModuleManager.getInstance().getModuleInstance(AuctionHouseModule.class);
        this.equipmentModule = ModuleManager.getInstance().getModuleInstance(EquipmentModule.class);
        this.levelingModule = ModuleManager.getInstance().getModuleInstance(LevelModule.class);
        this.playerStat = ModuleManager.getInstance().getModuleInstance(PlayerStatModule.class);
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

        // Create Player Head Item with player's skin
        Item playerHead = createPlayerHead(player, attributes);

        // Create Attribute Items
        Item vitalityItem = createStatItem("Vitality", attributes.getOrDefault("Vitality", 0), attributes.getOrDefault("HP", 0));
        Item intelligenceItem = createStatItem("Intelligence", attributes.getOrDefault("Intelligence", 0), attributes.getOrDefault("MANA", 0));
        Item dexterityItem = createStatItem("Dexterity", attributes.getOrDefault("Dexterity", 0), attributes.getOrDefault("AttackDamageDex", 0));
        Item strengthItem = createStatItem("Strength", attributes.getOrDefault("Strength", 0), attributes.getOrDefault("AttackDamage", 0));

        // Create Increment Buttons
        Item vitalityButton = createIncrementButton(attributePoints, "Vitality");
        Item intelligenceButton = createIncrementButton(attributePoints, "Intelligence");
        Item dexterityButton = createIncrementButton(attributePoints, "Dexterity");
        Item strengthButton = createIncrementButton(attributePoints, "Strength");

        // Attribute Points Item
        Item attributePointsItem = new SimpleItem(new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(Utils.getInstance().$("Attribute Points"))
                .addLoreLines(Utils.getInstance().$("Points: " + attributePoints))
                .addAllItemFlags());


        // Static items
        Item back = new SimpleItem(new ItemBuilder(Material.BARRIER)
                .setDisplayName(ChatColor.RED + "Go Back")
                .addLoreLines("", "Click to get back to the main menu.", "")
                .addAllItemFlags()
                .addEnchantment(Enchantment.UNBREAKING, 1, true))
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player clickPlayer, @NotNull InventoryClickEvent event) {
                Window window = event.getView().getTopInventory().getHolder() instanceof Window ? (Window) event.getView().getTopInventory().getHolder() : null;
                if (window != null) {
                    window.close();
                }

                MainMenu mainMenu = new MainMenu(auctionHouse, equipmentModule, levelingModule, playerStat, questModule, friendsModule, partyModule);
                mainMenu.openGUI(clickPlayer);
            }
        };


        // Filler
        Item filler = new SimpleItem(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .addAllItemFlags());

        // Build the GUI
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # H # # # #",
                        "# V # I # D # S #",
                        "# v # i # d # s #",
                        "# # # # # # # # #",
                        "# # # # # # # # #",
                        "B # # # # # # # A"
                )
                .addIngredient('#', filler)
                .addIngredient('H', playerHead)
                .addIngredient('V', vitalityItem)
                .addIngredient('I', intelligenceItem)
                .addIngredient('D', dexterityItem)
                .addIngredient('S', strengthItem)
                .addIngredient('v', vitalityButton)
                .addIngredient('i', intelligenceButton)
                .addIngredient('d', dexterityButton)
                .addIngredient('s', strengthButton)
                .addIngredient('A', attributePointsItem)
                .addIngredient('B', back)
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ChatColor.GREEN + "Player Stats")
                .setGui(gui)
                .build();
        window.open();
    }

    private Item createPlayerHead(Player player, Map<String, Integer> attributes) {
        ItemStack headStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) headStack.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(Utils.getInstance().$(player.getName()));
        skullMeta.setLore(java.util.Arrays.asList(
                Utils.getInstance().$("Vitality: " + attributes.getOrDefault("Vitality", 0)),
                Utils.getInstance().$("Intelligence: " + attributes.getOrDefault("Intelligence", 0)),
                Utils.getInstance().$("Dexterity: " + attributes.getOrDefault("Dexterity", 0)),
                Utils.getInstance().$("Strength: " + attributes.getOrDefault("Strength", 0)),
                Utils.getInstance().$("HP: " + attributes.getOrDefault("HP", 0)),
                Utils.getInstance().$("Mana: " + attributes.getOrDefault("MANA", 0))
        ));
        skullMeta.addItemFlags(ItemFlag.values());
        headStack.setItemMeta(skullMeta);

        return new SimpleItem(headStack);
    }

    private Item createStatItem(String name, int attribute, int stat) {
        return new SimpleItem(new ItemBuilder(getMaterialForAttribute(name))
                .setDisplayName(Utils.getInstance().$(name))
                .addLoreLines(
                        Utils.getInstance().$("Attribute: " + attribute),
                        Utils.getInstance().$("Stat: " + stat)
                )
                .addAllItemFlags());
    }

    private Material getMaterialForAttribute(String name) {
        switch (name) {
            case "Vitality":
                return Material.APPLE;
            case "Intelligence":
                return Material.DRAGON_BREATH;
            case "Dexterity":
                return Material.ARROW;
            case "Strength":
                return Material.IRON_SWORD;
            default:
                return Material.BARRIER;
        }
    }

    private Item createIncrementButton(int attributePoints, String attributeName) {
        Material buttonMaterial = attributePoints > 0 ? Material.SUNFLOWER : Material.BEDROCK;
        String displayName = attributePoints > 0 ? Utils.getInstance().$("Increase " + attributeName) : Utils.getInstance().$("[NO POINTS]");
        return new SimpleItem(new ItemBuilder(buttonMaterial)
                .setDisplayName(displayName)
                .addAllItemFlags())
        {
            @Override
            public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
                if (attributePoints > 0) {
                    // Increase the attribute using PlayerStatModule logic
                    if (playerStatModule != null) {
                        playerStatModule.increaseAttribute(player, "Increase " + attributeName);
                        // Re-open GUI to show updated stats
                        openCharacterGUI(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "Stat module not found.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "No attribute points available.");
                }
            }
        };
    }
}
